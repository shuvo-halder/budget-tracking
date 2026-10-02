package com.engrshuvo.financemanager.data.model

enum class LoanType {
    LENT,       // Money given to someone (Receivable / You are owed)
    BORROWED    // Money taken from someone (Payable / You owe)
}

enum class LoanStatus {
    ACTIVE,
    SETTLED
}
