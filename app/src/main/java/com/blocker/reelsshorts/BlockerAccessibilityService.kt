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
 * Por isso a detecção aqui é feita de forma heurística, combinando vários
 * sinais (resource-id, content-description, nome de classe) em vez de
 * depender de um único identificador exato. Se algum dia parar de
 * funcionar após uma atualização do app, normalmente basta adicionar
 * novos termos às listas KEYWORDS_* abaixo.
 */
class BlockerAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private var lastBlockTimeMillis = 0L
    private val cooldownMillis = 800L // evita disparos repetidos em sequência

    companion object {
        private const val TAG = "ReelsShortsBlocker"

        private const val PKG_INSTAGRAM = "com.instagram.android"
        private const val PKG_YOUTUBE = "com.google.android.youtube"

        // Termos usados para identificar a tela/aba de Reels dentro do Instagram.
        // Internamente o Instagram ainda usa o nome "clips" para Reels em vários ids.
        private val INSTAGRAM_REEL_ID_KEYWORDS = listOf(
            "clips_viewer_view_pager",
            "clips_swipe_refresh_container",
            "clips_tab",
            "reels_tray",
            "clips_viewer",
            "reel_viewer"
        )
        private val INSTAGRAM_REEL_DESC_KEYWORDS = listOf(
            "reels", "reel"
        )

        // Termos usados para identificar a tela/aba de Shorts dentro do YouTube.
        private val YOUTUBE_SHORTS_ID_KEYWORDS = listOf(
            "reel_recycler",
            "reel_player",
            "shorts_player",
            "reel_watch",
            "shorts_shelf"
        )
        private val YOUTUBE_SHORTS_DESC_KEYWORDS = listOf(
            "shorts", "short"
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
        // 1) Clique direto na aba/botão "Reels" da barra inferior ou de um card de reel.
        val source = event.source
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && source != null) {
            val desc = source.contentDescription?.toString()?.lowercase()
            if (desc != null && INSTAGRAM_REEL_DESC_KEYWORDS.any { desc.contains(it) }) {
                return true
            }
        }

        // 2) Varredura da árvore de nós à procura de identificadores conhecidos da tela de Reels.
        val root = rootInActiveWindow ?: return false
        return nodeTreeContainsKeyword(
            root = root,
            idKeywords = INSTAGRAM_REEL_ID_KEYWORDS,
            descKeywords = emptyList() // descrição sozinha gera falsos positivos demais no feed normal
        )
    }

    private fun isYoutubeShowingShorts(event: AccessibilityEvent): Boolean {
        val source = event.source
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && source != null) {
            val desc = source.contentDescription?.toString()?.lowercase()
            if (desc != null && YOUTUBE_SHORTS_DESC_KEYWORDS.any { desc.contains(it) }) {
                return true
            }
        }

        val root = rootInActiveWindow ?: return false
        return nodeTreeContainsKeyword(
            root = root,
            idKeywords = YOUTUBE_SHORTS_ID_KEYWORDS,
            descKeywords = emptyList()
        )
    }

    /**
     * Percorre a árvore de nós de acessibilidade (com limite de segurança)
     * procurando um resource-id ou content-description que bata com alguma
     * das palavras-chave informadas.
     */
    private fun nodeTreeContainsKeyword(
        root: AccessibilityNodeInfo,
        idKeywords: List<String>,
        descKeywords: List<String>
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

            if (descKeywords.isNotEmpty()) {
                val desc = node.contentDescription?.toString()?.lowercase()
                if (desc != null && descKeywords.any { desc.contains(it) }) {
                    return true
                }
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
