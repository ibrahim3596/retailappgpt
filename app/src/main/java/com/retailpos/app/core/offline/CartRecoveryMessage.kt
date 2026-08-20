package com.retailpos.app.core.offline

object CartRecoveryMessage {
    fun summarize(result: CartRecoveryResult): String? {
        if (result.issues.isEmpty()) return null
        val missing = result.issues.count { it.type == CartRecoveryIssueType.MISSING_PRODUCT }
        val archived = result.issues.count { it.type == CartRecoveryIssueType.ARCHIVED_PRODUCT }
        val stock = result.issues.count { it.type == CartRecoveryIssueType.INSUFFICIENT_STOCK }
        val invalid = result.issues.count { it.type == CartRecoveryIssueType.INVALID_PRICING }
        return buildList {
            if (missing > 0) add("$missing product(s) no longer exist")
            if (archived > 0) add("$archived product(s) are archived")
            if (stock > 0) add("$stock product(s) no longer have enough stock")
            if (invalid > 0) add("$invalid product(s) have invalid saved pricing")
        }.joinToString("; ").replaceFirstChar { it.uppercase() } + ". Removed unsafe lines from the active bill."
    }
}
