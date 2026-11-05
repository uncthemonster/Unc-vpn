package com.example.myvpn

data class Country(val code: String, val name: String) {
    val flag: String
        get() = code.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}

val COUNTRIES = listOf(
    Country("US", "United States"), Country("GB", "United Kingdom"), Country("DE", "Germany"),
    Country("NL", "Netherlands"), Country("FR", "France"), Country("CA", "Canada"),
    Country("SG", "Singapore"), Country("JP", "Japan"), Country("IN", "India"),
    Country("AE", "UAE"), Country("AU", "Australia"), Country("SE", "Sweden"),
    Country("CH", "Switzerland"), Country("FI", "Finland"), Country("PL", "Poland"),
    Country("ES", "Spain"), Country("IT", "Italy"), Country("BR", "Brazil"),
    Country("KR", "South Korea"), Country("HK", "Hong Kong"), Country("TR", "Turkey"),
    Country("ZA", "South Africa"), Country("PK", "Pakistan"), Country("RO", "Romania"),
    Country("NO", "Norway"), Country("IE", "Ireland"), Country("MX", "Mexico"),
    Country("AT", "Austria"), Country("DK", "Denmark"), Country("CZ", "Czechia"),
)

fun countryOf(code: String) = COUNTRIES.find { it.code == code } ?: Country(code, code)
