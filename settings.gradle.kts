include(":core")

// Strictly load ONLY the 5 requested extensions
val requestedExtensions = listOf("tnaflix", "eporner", "xhamster", "spankbang", "bigfuck")
requestedExtensions.forEach { include("src:en:$it") }
