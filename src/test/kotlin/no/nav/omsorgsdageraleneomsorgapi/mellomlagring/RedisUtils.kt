package no.nav.omsorgsdageraleneomsorgapi.mellomlagring

import com.github.fppt.jedismock.RedisServer

internal fun RedisServer.started() = apply { start() }