package msw.server.core.model.props

enum class GameMode(val modeName: String, val id: Int) {
    SURVIVAL("survival", 0),
    CREATIVE("creative", 1),
    ADVENTURE("adventure", 2),
    SPECTATOR("spectator", 3)
}