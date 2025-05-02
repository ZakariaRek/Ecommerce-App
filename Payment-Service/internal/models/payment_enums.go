package models

// PaymentMethod enum
type PaymentMethod string

const (
	CreditCard   PaymentMethod = "CREDIT_CARD"
	DebitCard    PaymentMethod = "DEBIT_CARD"
	PayPal       PaymentMethod = "PAYPAL"
	BankTransfer PaymentMethod = "BANK_TRANSFER"
	Crypto       PaymentMethod = "CRYPTO"
	Points       PaymentMethod = "POINTS"
	GiftCard     PaymentMethod = "GIFT_CARD"
)

// PaymentStatus enum
type PaymentStatus string

const (
	Pending           PaymentStatus = "PENDING"
	Completed         PaymentStatus = "COMPLETED"
	Failed            PaymentStatus = "FAILED"
	Refunded          PaymentStatus = "REFUNDED"
	PartiallyRefunded PaymentStatus = "PARTIALLY_REFUNDED"
)
