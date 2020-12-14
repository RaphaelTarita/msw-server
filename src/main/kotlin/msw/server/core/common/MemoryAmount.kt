package msw.server.core.common

class MemoryAmount(val amount: Long, val unit: MemoryUnit) {
    override fun toString(): String {
        return "$amount${unit.rep}"
    }
}