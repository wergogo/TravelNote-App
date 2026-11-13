package com.wergogo.travelnote

/** Shared text for the B7 `@Ignore`s (annotation arguments must be compile-time constants). */
const val B7_RULE =
    "Bug B7: input parsed with toDoubleOrNull(); rule: a comma + 1-2 trailing digits is the decimal " +
        "separator (\"12,50\" -> 12.5, \"7,5\" -> 7.5), any other comma is a thousands separator " +
        "(\"1,500\" -> 1500.0, \"1,500.25\" -> 1500.25)"
