package com.example.bleapp.data

import androidx.compose.ui.geometry.Offset

/**
 * Граф навигации по плану: узлы в нормализованных image-space координатах
 * (y=0 — верх) и неориентированные рёбра.
 *
 * Используется симуляцией движения, чтобы пользователь шёл по коридорам
 * и через дверные проёмы, а не сквозь стены.
 */
data class PlanNavGraph(
    val nodes: List<Offset>,
    /** Пары индексов узлов; ребро двунаправленное. */
    val edges: List<Pair<Int, Int>>
) {
    private val adjacency: List<List<Int>> = run {
        val acc = MutableList(nodes.size) { mutableListOf<Int>() }
        for ((a, b) in edges) {
            acc[a].add(b)
            acc[b].add(a)
        }
        acc.map { it.toList() }
    }

    fun neighbors(i: Int): List<Int> = adjacency[i]

    fun nearestNode(p: Offset): Int {
        var best = 0
        var bestD = Float.MAX_VALUE
        for (i in nodes.indices) {
            val dx = nodes[i].x - p.x
            val dy = nodes[i].y - p.y
            val d = dx * dx + dy * dy
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }

    /** BFS-кратчайший путь по числу рёбер. Возвращает список узлов от from к to включительно. */
    fun shortestPath(from: Int, to: Int): List<Int> {
        if (from == to) return listOf(from)
        val prev = IntArray(nodes.size) { -1 }
        val visited = BooleanArray(nodes.size)
        val queue = ArrayDeque<Int>()
        queue.addLast(from); visited[from] = true
        while (queue.isNotEmpty()) {
            val v = queue.removeFirst()
            if (v == to) break
            for (n in adjacency[v]) {
                if (!visited[n]) { visited[n] = true; prev[n] = v; queue.addLast(n) }
            }
        }
        if (!visited[to]) return listOf(from)
        val path = ArrayDeque<Int>()
        var cur = to
        while (cur != -1) { path.addFirst(cur); cur = prev[cur] }
        return path.toList()
    }
}

/**
 * Граф для 1 этажа Интеллект-ГНСС (ig_f1).
 *
 * Координаты подобраны эмпирически по SVG-плану:
 * — главный коридор идёт горизонтально, центр около y≈0.725 (между северными
 *   комнатами с южной стеной y≈0.713 и южными комнатами с северной стеной y≈0.737);
 * — северные/южные узлы — заходы в типовые комнаты через дверные проёмы.
 * При желании можно сдвинуть точки/добавить ответвления.
 */
private val IG_F1_NODES = listOf(
    Offset(0.130f, 0.725f), //  0  C0 — западный конец коридора
    Offset(0.220f, 0.725f), //  1  C1
    Offset(0.320f, 0.725f), //  2  C2
    Offset(0.440f, 0.725f), //  3  C3
    Offset(0.560f, 0.725f), //  4  C4
    Offset(0.680f, 0.725f), //  5  C5
    Offset(0.790f, 0.725f), //  6  C6
    Offset(0.870f, 0.725f), //  7  C7 — восточный конец
    Offset(0.150f, 0.660f), //  8  N0 — вход в северную комнату (запад)
    Offset(0.220f, 0.640f), //  9  N1
    Offset(0.440f, 0.640f), // 10  N2
    Offset(0.560f, 0.640f), // 11  N3
    Offset(0.680f, 0.640f), // 12  N4
    Offset(0.840f, 0.640f), // 13  N5 — вход в северную комнату (восток)
    Offset(0.150f, 0.800f), // 14  S0 — вход в южную комнату (запад)
    Offset(0.220f, 0.815f), // 15  S1
    Offset(0.440f, 0.815f), // 16  S2
    Offset(0.560f, 0.815f), // 17  S3
    Offset(0.680f, 0.815f), // 18  S4
    Offset(0.840f, 0.800f), // 19  S5 — вход в южную комнату (восток)
)

private val IG_F1_EDGES = listOf(
    // главный коридор
    0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7,
    // северные ответвления (двери)
    0 to 8, 1 to 9, 3 to 10, 4 to 11, 5 to 12, 7 to 13,
    // южные ответвления (двери)
    0 to 14, 1 to 15, 3 to 16, 4 to 17, 5 to 18, 7 to 19,
)

val planNavGraphs: Map<String, PlanNavGraph> = mapOf(
    "ig_f1" to PlanNavGraph(IG_F1_NODES, IG_F1_EDGES),
)
