package com.example.core.versions

import java.net.URL

class DownloadException : Exception {
    constructor(url: URL, msg: String) : super("For URL $url: $msg")
    constructor(msg: String) : super(msg)
}