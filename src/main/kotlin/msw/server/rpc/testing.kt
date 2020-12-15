package msw.server.rpc

fun testing() {
    val point = Point {
        latitude = 13
        longitude = 15
    }

    val note = RouteNote {
        msg = "Hello, World!"
    }
}