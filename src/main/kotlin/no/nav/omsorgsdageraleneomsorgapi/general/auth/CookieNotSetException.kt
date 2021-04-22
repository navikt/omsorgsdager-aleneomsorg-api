package no.nav.omsorgsdageraleneomsorgapi.general.auth

class CookieNotSetException(cookieName : String) : RuntimeException("Ingen cookie med navnet '$cookieName' satt.")