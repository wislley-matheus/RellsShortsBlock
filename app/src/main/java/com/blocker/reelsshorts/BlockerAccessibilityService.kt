package com.blocker.reelsshorts

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Serviço de acessibilidade responsável por detectar quando o usuário
 * entra na tela de Reels (Instagram) ou Shorts (YouTube) e "voltar"
 * automaticamente, na prática impedindo o uso desses recursos.
 *
 * Observação importante: o Instagram e o YouTube mudam a estrutura interna
 * de telas (resource-id, texto etc.) com frequência a cada atualização.
 * Por isso a detecção aqui é feita de forma heurística, procurando por
 * identificadores exclusivos da tela cheia de Reels/Shorts (não do feed
 * normal). Se algum dia parar de funcionar após uma atualização do app,
 * normalmente basta adicionar novos termos às listas KEYWORDS abaixo.
 */
class BlockerAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private var lastBlockTimeMillis = 0L
    private val cooldownMillis = 800L // evita disparos repetidos em sequência

    companion object {
        private const val TAG = "ReelsShortsBlocker"

        private const val PKG_INSTAGRAM = "com.instagram.android"
        private const val PKG_YOUTUBE = "com.google.android.youtube"

        // Termos usados para identificar a TELA EM TELA CHEIA de Reels dentro do
        // Instagram (não o feed normal, que também contém prévias/trays de Reels
        // e por isso não pode entrar aqui, ou o app inteiro fica bloqueado).
        // Internamente o Instagram ainda usa o nome "clips" para Reels em vários ids.
        private val INSTAGRAM_REEL_ID_KEYWORDS = listOf(
            "clips_viewer_view_pager",
            "clips_swipe_refresh_container",
            "clips_viewer_fragment"
        )

        // Termos usados para identificar a TELA EM TELA CHEIA de Shorts dentro do YouTube
        // (não a prateleira de Shorts que aparece na home normal do app).
        private val YOUTUBE_SHORTS_ID_KEYWORDS = listOf(
            "reel_player_page_container",
            "reel_recycler",
            "reel_watch_player"
        )

        private const val MAX_NODES_TO_SCAN = 400
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return

        when (packageName) {
            PKG_INSTAGRAM -> {
                if (!PrefsManager.isInstagramBlockEnabled(applicationContext)) return
                if (isInstagramShowingReels(event)) {
                    blockAndGoBack(isReels = true)
                }
            }
            PKG_YOUTUBE -> {
                if (!PrefsManager.isYoutubeBlockEnabled(applicationContext)) return
                if (isYoutubeShowingShorts(event)) {
                    blockAndGoBack(isReels = false)
                }
            }
        }
    }

    override fun onInterrupt() {
        // Nada a fazer.
    }

    // ---------- Detecção ----------

    private fun isInstagramShowingReels(event: AccessibilityEvent): Boolean {
        val root = rootInActiveWindow ?: return false
        return nodeTreeContainsIdKeyword(root, INSTAGRAM_REEL_ID_KEYWORDS)
    }

    private fun isYoutubeShowingShorts(event: AccessibilityEvent): Boolean {
        val root = rootInActiveWindow ?: return false
        return nodeTreeContainsIdKeyword(root, YOUTUBE_SHORTS_ID_KEYWORDS)
    }

    /**
     * Percorre a árvore de nós de acessibilidade (com limite de segurança)
     * procurando um resource-id que bata com alguma das palavras-chave informadas.
     * Usamos só resource-id (não content-description) porque descrições de posts
     * comuns do feed às vezes contêm palavras como "reel"/"shorts" na legenda,
     * o que gerava falso positivo e bloqueava o app inteiro.
     */
    private fun nodeTreeContainsIdKeyword(
        root: AccessibilityNodeInfo,
        idKeywords: List<String>
    ): Boolean {
        var scanned = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty() && scanned < MAX_NODES_TO_SCAN) {
            val node = queue.removeFirst()
            scanned++

            val viewId = node.viewIdResourceName?.lowercase()
            if (viewId != null && idKeywords.any { viewId.contains(it) }) {
                return true
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

    // ---------- Ação de bloqueio ----------

    private fun blockAndGoBack(isReels: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastBlockTimeMillis < cooldownMillis) return
        lastBlockTimeMillis = now

        Log.d(TAG, if (isReels) "Reels detectado, voltando..." else "Shorts detectado, voltando...")

        performGlobalAction(GLOBAL_ACTION_BACK)

        // Segunda tentativa curta, caso a primeira tela de trás ainda seja
        // parte do fluxo de Reels/Shorts (ex.: transição animada).
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
        }, 350)

        handler.post {
            if (isReels) {
                PrefsManager.incrementReelsBlockedCount(applicationContext)
            } else {
                PrefsManager.incrementShortsBlockedCount(applicationContext)
            }
        }

        val message = if (isReels) "Reels bloqueado" else "Shorts bloqueado"
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}
