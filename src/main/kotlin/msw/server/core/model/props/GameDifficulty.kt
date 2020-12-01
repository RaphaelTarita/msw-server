package msw.server.core.model.props

enum class GameDifficulty(val difficultyName: String, val id: Int) {
    PEACEFUL("peaceful", 0),
    EASY("easy", 1),
    NORMAL("normal", 2),
    HARD("hard", 3)
}