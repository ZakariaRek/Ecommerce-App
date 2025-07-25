// Payment-Service/internal/repository/enhanced_payment_repository.go
package repository

import (
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

// Enhanced PaymentRepository interface with analytics methods
type EnhancedPaymentRepository interface {
	PaymentRepository

	// Analytics and filtering methods
	FindWithFilters(filters PaymentFilters) ([]*models.Payment, error)
	FindByDateRange(startDate, endDate time.Time) ([]*models.Payment, error)
	FindByStatus(status models.PaymentStatus) ([]*models.Payment, error)
	FindByMethod(method models.PaymentMethod) ([]*models.Payment, error)
	FindByStatusAndDateRange(status models.PaymentStatus, startDate, endDate time.Time) ([]*models.Payment, error)

	// Aggregation methods for analytics
	GetDailyStats(date time.Time) (*DailyStats, error)
	GetWeeklyStats(startDate time.Time) (*WeeklyStats, error)
	GetMonthlyStats(year int, month int) (*MonthlyStats, error)
	GetRevenueByPeriod(startDate, endDate time.Time) (float64, error)
	GetTransactionCountByStatus() (map[models.PaymentStatus]int64, error)
	GetRevenueByPaymentMethod() (map[models.PaymentMethod]float64, error)
	GetAverageTransactionValue() (float64, error)
	GetTopCustomersBySpending(limit int) ([]*CustomerSpending, error)

	// Pagination methods
	FindWithPagination(page, limit int, filters PaymentFilters) (*PaginatedPayments, error)
}

// PaymentFilters represents filtering options for payments
type PaymentFilters struct {
	Status     *models.PaymentStatus
	Method     *models.PaymentMethod
	StartDate  *time.Time
	EndDate    *time.Time
	OrderID    *uuid.UUID
	AmountMin  *float64
	AmountMax  *float64
	CustomerID *string
}

// DailyStats represents daily payment statistics
type DailyStats struct {
	Date               time.Time `json:"date"`
	TotalTransactions  int64     `json:"totalTransactions"`
	SuccessfulPayments int64     `json:"successfulPayments"`
	FailedPayments     int64     `json:"failedPayments"`
	PendingPayments    int64     `json:"pendingPayments"`
	RefundedPayments   int64     `json:"refundedPayments"`
	TotalRevenue       float64   `json:"totalRevenue"`
	AverageAmount      float64   `json:"averageAmount"`
	SuccessRate        float64   `json:"successRate"`
}

// WeeklyStats represents weekly payment statistics
type WeeklyStats struct {
	WeekStart          time.Time     `json:"weekStart"`
	WeekEnd            time.Time     `json:"weekEnd"`
	TotalTransactions  int64         `json:"totalTransactions"`
	SuccessfulPayments int64         `json:"successfulPayments"`
	TotalRevenue       float64       `json:"totalRevenue"`
	AverageAmount      float64       `json:"averageAmount"`
	SuccessRate        float64       `json:"successRate"`
	DailyBreakdown     []*DailyStats `json:"dailyBreakdown"`
}

// MonthlyStats represents monthly payment statistics
type MonthlyStats struct {
	Year               int            `json:"year"`
	Month              int            `json:"month"`
	TotalTransactions  int64          `json:"totalTransactions"`
	SuccessfulPayments int64          `json:"successfulPayments"`
	TotalRevenue       float64        `json:"totalRevenue"`
	AverageAmount      float64        `json:"averageAmount"`
	SuccessRate        float64        `json:"successRate"`
	WeeklyBreakdown    []*WeeklyStats `json:"weeklyBreakdown"`
}

// CustomerSpending represents customer spending analytics
type CustomerSpending struct {
	OrderID          string    `json:"orderID"` // Using OrderID as customer identifier
	TotalSpent       float64   `json:"totalSpent"`
	TransactionCount int64     `json:"transactionCount"`
	AverageAmount    float64   `json:"averageAmount"`
	LastTransaction  time.Time `json:"lastTransaction"`
}

// PaginatedPayments represents paginated payment results
type PaginatedPayments struct {
	Payments   []*models.Payment `json:"payments"`
	Total      int64             `json:"total"`
	Page       int               `json:"page"`
	Limit      int               `json:"limit"`
	TotalPages int               `json:"totalPages"`
}

// enhancedPaymentRepository implements EnhancedPaymentRepository
type enhancedPaymentRepository struct {
	*paymentRepository
}

// NewEnhancedPaymentRepository creates a new enhanced payment repository
func NewEnhancedPaymentRepository(db *gorm.DB) EnhancedPaymentRepository {
	return &enhancedPaymentRepository{
		paymentRepository: &paymentRepository{db: db},
	}
}

// FindWithFilters finds payments with advanced filtering
func (r *enhancedPaymentRepository) FindWithFilters(filters PaymentFilters) ([]*models.Payment, error) {
	var payments []*models.Payment

	query := r.db.Model(&models.Payment{})

	// Apply filters
	if filters.Status != nil {
		query = query.Where("status = ?", *filters.Status)
	}

	if filters.Method != nil {
		query = query.Where("method = ?", *filters.Method)
	}

	if filters.StartDate != nil {
		query = query.Where("created_at >= ?", *filters.StartDate)
	}

	if filters.EndDate != nil {
		query = query.Where("created_at <= ?", *filters.EndDate)
	}

	if filters.OrderID != nil {
		query = query.Where("order_id = ?", *filters.OrderID)
	}

	if filters.AmountMin != nil {
		query = query.Where("amount >= ?", *filters.AmountMin)
	}

	if filters.AmountMax != nil {
		query = query.Where("amount <= ?", *filters.AmountMax)
	}

	err := query.Order("created_at DESC").Find(&payments).Error
	return payments, err
}

// FindByDateRange finds payments within a date range
func (r *enhancedPaymentRepository) FindByDateRange(startDate, endDate time.Time) ([]*models.Payment, error) {
	var payments []*models.Payment
	err := r.db.Where("created_at BETWEEN ? AND ?", startDate, endDate).
		Order("created_at DESC").
		Find(&payments).Error
	return payments, err
}

// FindByStatus finds payments by status
func (r *enhancedPaymentRepository) FindByStatus(status models.PaymentStatus) ([]*models.Payment, error) {
	var payments []*models.Payment
	err := r.db.Where("status = ?", status).
		Order("created_at DESC").
		Find(&payments).Error
	return payments, err
}

// FindByMethod finds payments by payment method
func (r *enhancedPaymentRepository) FindByMethod(method models.PaymentMethod) ([]*models.Payment, error) {
	var payments []*models.Payment
	err := r.db.Where("method = ?", method).
		Order("created_at DESC").
		Find(&payments).Error
	return payments, err
}

// FindByStatusAndDateRange finds payments by status within a date range
func (r *enhancedPaymentRepository) FindByStatusAndDateRange(status models.PaymentStatus, startDate, endDate time.Time) ([]*models.Payment, error) {
	var payments []*models.Payment
	err := r.db.Where("status = ? AND created_at BETWEEN ? AND ?", status, startDate, endDate).
		Order("created_at DESC").
		Find(&payments).Error
	return payments, err
}

// GetDailyStats calculates daily payment statistics
func (r *enhancedPaymentRepository) GetDailyStats(date time.Time) (*DailyStats, error) {
	startOfDay := time.Date(date.Year(), date.Month(), date.Day(), 0, 0, 0, 0, date.Location())
	endOfDay := startOfDay.Add(24 * time.Hour)

	var stats DailyStats
	stats.Date = startOfDay

	// Get total transactions
	err := r.db.Model(&models.Payment{}).
		Where("created_at BETWEEN ? AND ?", startOfDay, endOfDay).
		Count(&stats.TotalTransactions).Error
	if err != nil {
		return nil, err
	}

	// Get successful payments
	err = r.db.Model(&models.Payment{}).
		Where("status = ? AND created_at BETWEEN ? AND ?", models.Completed, startOfDay, endOfDay).
		Count(&stats.SuccessfulPayments).Error
	if err != nil {
		return nil, err
	}

	// Get failed payments
	err = r.db.Model(&models.Payment{}).
		Where("status = ? AND created_at BETWEEN ? AND ?", models.Failed, startOfDay, endOfDay).
		Count(&stats.FailedPayments).Error
	if err != nil {
		return nil, err
	}

	// Get pending payments
	err = r.db.Model(&models.Payment{}).
		Where("status = ? AND created_at BETWEEN ? AND ?", models.Pending, startOfDay, endOfDay).
		Count(&stats.PendingPayments).Error
	if err != nil {
		return nil, err
	}

	// Get refunded payments
	err = r.db.Model(&models.Payment{}).
		Where("status IN ? AND created_at BETWEEN ? AND ?",
			[]models.PaymentStatus{models.Refunded, models.PartiallyRefunded},
			startOfDay, endOfDay).
		Count(&stats.RefundedPayments).Error
	if err != nil {
		return nil, err
	}

	// Get total revenue from successful payments
	err = r.db.Model(&models.Payment{}).
		Where("status = ? AND created_at BETWEEN ? AND ?", models.Completed, startOfDay, endOfDay).
		Select("COALESCE(SUM(amount), 0)").
		Scan(&stats.TotalRevenue).Error
	if err != nil {
		return nil, err
	}

	// Calculate average amount
	if stats.SuccessfulPayments > 0 {
		stats.AverageAmount = stats.TotalRevenue / float64(stats.SuccessfulPayments)
	}

	// Calculate success rate
	if stats.TotalTransactions > 0 {
		stats.SuccessRate = float64(stats.SuccessfulPayments) / float64(stats.TotalTransactions) * 100
	}

	return &stats, nil
}

// GetWeeklyStats calculates weekly payment statistics
func (r *enhancedPaymentRepository) GetWeeklyStats(startDate time.Time) (*WeeklyStats, error) {
	// Calculate week boundaries
	weekStart := startDate
	weekEnd := startDate.AddDate(0, 0, 7)

	var stats WeeklyStats
	stats.WeekStart = weekStart
	stats.WeekEnd = weekEnd

	// Get aggregated stats for the week
	err := r.db.Model(&models.Payment{}).
		Where("created_at BETWEEN ? AND ?", weekStart, weekEnd).
		Count(&stats.TotalTransactions).Error
	if err != nil {
		return nil, err
	}

	err = r.db.Model(&models.Payment{}).
		Where("status = ? AND created_at BETWEEN ? AND ?", models.Completed, weekStart, weekEnd).
		Count(&stats.SuccessfulPayments).Error
	if err != nil {
		return nil, err
	}

	err = r.db.Model(&models.Payment{}).
		Where("status = ? AND created_at BETWEEN ? AND ?", models.Completed, weekStart, weekEnd).
		Select("COALESCE(SUM(amount), 0)").
		Scan(&stats.TotalRevenue).Error
	if err != nil {
		return nil, err
	}

	if stats.SuccessfulPayments > 0 {
		stats.AverageAmount = stats.TotalRevenue / float64(stats.SuccessfulPayments)
	}

	if stats.TotalTransactions > 0 {
		stats.SuccessRate = float64(stats.SuccessfulPayments) / float64(stats.TotalTransactions) * 100
	}

	// Get daily breakdown
	stats.DailyBreakdown = make([]*DailyStats, 0, 7)
	for i := 0; i < 7; i++ {
		dayStats, err := r.GetDailyStats(weekStart.AddDate(0, 0, i))
		if err != nil {
			return nil, err
		}
		stats.DailyBreakdown = append(stats.DailyBreakdown, dayStats)
	}

	return &stats, nil
}

// GetMonthlyStats calculates monthly payment statistics
func (r *enhancedPaymentRepository) GetMonthlyStats(year int, month int) (*MonthlyStats, error) {
	monthStart := time.Date(year, time.Month(month), 1, 0, 0, 0, 0, time.UTC)
	monthEnd := monthStart.AddDate(0, 1, 0)

	var stats MonthlyStats
	stats.Year = year
	stats.Month = month

	// Get aggregated stats for the month
	err := r.db.Model(&models.Payment{}).
		Where("created_at BETWEEN ? AND ?", monthStart, monthEnd).
		Count(&stats.TotalTransactions).Error
	if err != nil {
		return nil, err
	}

	err = r.db.Model(&models.Payment{}).
		Where("status = ? AND created_at BETWEEN ? AND ?", models.Completed, monthStart, monthEnd).
		Count(&stats.SuccessfulPayments).Error
	if err != nil {
		return nil, err
	}

	err = r.db.Model(&models.Payment{}).
		Where("status = ? AND created_at BETWEEN ? AND ?", models.Completed, monthStart, monthEnd).
		Select("COALESCE(SUM(amount), 0)").
		Scan(&stats.TotalRevenue).Error
	if err != nil {
		return nil, err
	}

	if stats.SuccessfulPayments > 0 {
		stats.AverageAmount = stats.TotalRevenue / float64(stats.SuccessfulPayments)
	}

	if stats.TotalTransactions > 0 {
		stats.SuccessRate = float64(stats.SuccessfulPayments) / float64(stats.TotalTransactions) * 100
	}

	return &stats, nil
}

// GetRevenueByPeriod calculates total revenue for a period
func (r *enhancedPaymentRepository) GetRevenueByPeriod(startDate, endDate time.Time) (float64, error) {
	var revenue float64
	err := r.db.Model(&models.Payment{}).
		Where("status = ? AND created_at BETWEEN ? AND ?", models.Completed, startDate, endDate).
		Select("COALESCE(SUM(amount), 0)").
		Scan(&revenue).Error
	return revenue, err
}

// GetTransactionCountByStatus returns transaction counts grouped by status
func (r *enhancedPaymentRepository) GetTransactionCountByStatus() (map[models.PaymentStatus]int64, error) {
	type StatusCount struct {
		Status models.PaymentStatus
		Count  int64
	}

	var results []StatusCount
	err := r.db.Model(&models.Payment{}).
		Select("status, COUNT(*) as count").
		Group("status").
		Scan(&results).Error

	if err != nil {
		return nil, err
	}

	statusCounts := make(map[models.PaymentStatus]int64)
	for _, result := range results {
		statusCounts[result.Status] = result.Count
	}

	return statusCounts, nil
}

// GetRevenueByPaymentMethod returns revenue grouped by payment method
func (r *enhancedPaymentRepository) GetRevenueByPaymentMethod() (map[models.PaymentMethod]float64, error) {
	type MethodRevenue struct {
		Method  models.PaymentMethod
		Revenue float64
	}

	var results []MethodRevenue
	err := r.db.Model(&models.Payment{}).
		Select("method, COALESCE(SUM(amount), 0) as revenue").
		Where("status = ?", models.Completed).
		Group("method").
		Scan(&results).Error

	if err != nil {
		return nil, err
	}

	methodRevenue := make(map[models.PaymentMethod]float64)
	for _, result := range results {
		methodRevenue[result.Method] = result.Revenue
	}

	return methodRevenue, nil
}

// GetAverageTransactionValue calculates the average transaction value
func (r *enhancedPaymentRepository) GetAverageTransactionValue() (float64, error) {
	var avgAmount float64
	err := r.db.Model(&models.Payment{}).
		Where("status = ?", models.Completed).
		Select("COALESCE(AVG(amount), 0)").
		Scan(&avgAmount).Error
	return avgAmount, err
}

// GetTopCustomersBySpending returns top customers by spending (using order_id as customer identifier)
func (r *enhancedPaymentRepository) GetTopCustomersBySpending(limit int) ([]*CustomerSpending, error) {
	var results []*CustomerSpending

	err := r.db.Model(&models.Payment{}).
		Select(`
			order_id,
			COALESCE(SUM(amount), 0) as total_spent,
			COUNT(*) as transaction_count,
			COALESCE(AVG(amount), 0) as average_amount,
			MAX(created_at) as last_transaction
		`).
		Where("status = ?", models.Completed).
		Group("order_id").
		Order("total_spent DESC").
		Limit(limit).
		Scan(&results).Error

	return results, err
}

// FindWithPagination finds payments with pagination and filters
func (r *enhancedPaymentRepository) FindWithPagination(page, limit int, filters PaymentFilters) (*PaginatedPayments, error) {
	var payments []*models.Payment
	var total int64

	// Build query with filters
	query := r.db.Model(&models.Payment{})

	if filters.Status != nil {
		query = query.Where("status = ?", *filters.Status)
	}

	if filters.Method != nil {
		query = query.Where("method = ?", *filters.Method)
	}

	if filters.StartDate != nil {
		query = query.Where("created_at >= ?", *filters.StartDate)
	}

	if filters.EndDate != nil {
		query = query.Where("created_at <= ?", *filters.EndDate)
	}

	if filters.OrderID != nil {
		query = query.Where("order_id = ?", *filters.OrderID)
	}

	if filters.AmountMin != nil {
		query = query.Where("amount >= ?", *filters.AmountMin)
	}

	if filters.AmountMax != nil {
		query = query.Where("amount <= ?", *filters.AmountMax)
	}

	// Get total count
	err := query.Count(&total).Error
	if err != nil {
		return nil, err
	}

	// Calculate offset
	offset := (page - 1) * limit

	// Get paginated results
	err = query.Order("created_at DESC").
		Offset(offset).
		Limit(limit).
		Find(&payments).Error

	if err != nil {
		return nil, err
	}

	// Calculate total pages
	totalPages := int(total) / limit
	if int(total)%limit != 0 {
		totalPages++
	}

	return &PaginatedPayments{
		Payments:   payments,
		Total:      total,
		Page:       page,
		Limit:      limit,
		TotalPages: totalPages,
	}, nil
}
