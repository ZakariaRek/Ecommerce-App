// Payment-Service/internal/handler/analytics_handler.go
package handler

import (
	"encoding/json"
	"net/http"
	"strconv"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service"
	"github.com/go-chi/chi/v5"
)

// AnalyticsHandler handles analytics and reporting endpoints for admin dashboard
type AnalyticsHandler struct {
	paymentService      service.PaymentService
	orderPaymentService service.OrderPaymentService
	invoiceService      service.InvoiceService
}

// NewAnalyticsHandler creates a new analytics handler
func NewAnalyticsHandler(
	paymentService service.PaymentService,
	orderPaymentService service.OrderPaymentService,
	invoiceService service.InvoiceService,
) *AnalyticsHandler {
	return &AnalyticsHandler{
		paymentService:      paymentService,
		orderPaymentService: orderPaymentService,
		invoiceService:      invoiceService,
	}
}

// RegisterRoutes registers analytics routes
func (h *AnalyticsHandler) RegisterRoutes(r chi.Router) {
	r.Route("/analytics", func(r chi.Router) {
		// Dashboard metrics
		r.Get("/dashboard", h.GetDashboardMetrics)
		r.Get("/dashboard/{period}", h.GetDashboardMetricsByPeriod)

		// Revenue analytics
		r.Get("/revenue", h.GetRevenueAnalytics)
		r.Get("/revenue/{period}", h.GetRevenueByPeriod)

		// Transaction analytics
		r.Get("/transactions", h.GetTransactionAnalytics)
		r.Get("/transactions/success-rate", h.GetSuccessRateAnalytics)
		r.Get("/transactions/methods", h.GetPaymentMethodAnalytics)

		// Gateway performance
		r.Get("/gateways", h.GetGatewayPerformance)

		// Customer analytics
		r.Get("/customers", h.GetCustomerAnalytics)

		// Reports
		r.Get("/reports/daily", h.GetDailyReport)
		r.Get("/reports/weekly", h.GetWeeklyReport)
		r.Get("/reports/monthly", h.GetMonthlyReport)

		// Export endpoints
		r.Get("/export/payments", h.ExportPayments)
		r.Get("/export/revenue", h.ExportRevenue)
	})
}

// DashboardMetrics represents the main dashboard metrics
type DashboardMetrics struct {
	Revenue struct {
		Today  float64 `json:"today"`
		Week   float64 `json:"week"`
		Month  float64 `json:"month"`
		Change float64 `json:"change"`
	} `json:"revenue"`
	Transactions struct {
		Total       int     `json:"total"`
		Successful  int     `json:"successful"`
		Failed      int     `json:"failed"`
		Pending     int     `json:"pending"`
		SuccessRate float64 `json:"successRate"`
	} `json:"transactions"`
	Refunds struct {
		Count  int     `json:"count"`
		Amount float64 `json:"amount"`
		Rate   float64 `json:"rate"`
	} `json:"refunds"`
	Invoices struct {
		Generated int `json:"generated"`
		Sent      int `json:"sent"`
		Paid      int `json:"paid"`
	} `json:"invoices"`
	Alerts []Alert `json:"alerts"`
}

type Alert struct {
	Type    string `json:"type"`
	Message string `json:"message"`
	Time    string `json:"time"`
}

// GetDashboardMetrics returns key metrics for the admin dashboard
func (h *AnalyticsHandler) GetDashboardMetrics(w http.ResponseWriter, r *http.Request) {
	// Get all payments (in a real implementation, you'd filter by date)
	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	metrics := h.calculateDashboardMetrics(payments)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(metrics)
}

// GetDashboardMetricsByPeriod returns metrics for a specific period
func (h *AnalyticsHandler) GetDashboardMetricsByPeriod(w http.ResponseWriter, r *http.Request) {
	period := chi.URLParam(r, "period")

	// Filter payments by period (simplified implementation)
	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// Apply period filter
	filteredPayments := h.filterPaymentsByPeriod(payments, period)
	metrics := h.calculateDashboardMetrics(filteredPayments)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(metrics)
}

// GetRevenueAnalytics returns revenue analytics
func (h *AnalyticsHandler) GetRevenueAnalytics(w http.ResponseWriter, r *http.Request) {
	period := r.URL.Query().Get("period")
	groupBy := r.URL.Query().Get("groupBy")

	if period == "" {
		period = "month"
	}
	if groupBy == "" {
		groupBy = "day"
	}

	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	revenueData := h.calculateRevenueAnalytics(payments, period, groupBy)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(revenueData)
}

// GetRevenueByPeriod returns revenue for a specific period
func (h *AnalyticsHandler) GetRevenueByPeriod(w http.ResponseWriter, r *http.Request) {
	period := chi.URLParam(r, "period")

	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	filteredPayments := h.filterPaymentsByPeriod(payments, period)

	totalRevenue := 0.0
	for _, payment := range filteredPayments {
		if payment.Status == models.Completed {
			totalRevenue += payment.Amount
		}
	}

	response := map[string]interface{}{
		"period":  period,
		"revenue": totalRevenue,
		"count":   len(filteredPayments),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetTransactionAnalytics returns transaction analytics
func (h *AnalyticsHandler) GetTransactionAnalytics(w http.ResponseWriter, r *http.Request) {
	period := r.URL.Query().Get("period")
	if period == "" {
		period = "month"
	}

	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	analytics := h.calculateTransactionAnalytics(payments, period)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(analytics)
}

// GetSuccessRateAnalytics returns success rate analytics by various dimensions
func (h *AnalyticsHandler) GetSuccessRateAnalytics(w http.ResponseWriter, r *http.Request) {
	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	successRates := h.calculateSuccessRates(payments)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(successRates)
}

// GetPaymentMethodAnalytics returns analytics by payment method
func (h *AnalyticsHandler) GetPaymentMethodAnalytics(w http.ResponseWriter, r *http.Request) {
	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	methodAnalytics := h.calculatePaymentMethodAnalytics(payments)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(methodAnalytics)
}

// GetGatewayPerformance returns gateway performance metrics
func (h *AnalyticsHandler) GetGatewayPerformance(w http.ResponseWriter, r *http.Request) {
	// Mock gateway performance data (in real implementation, get from monitoring)
	performance := map[string]interface{}{
		"gateways": []map[string]interface{}{
			{
				"name":        "Stripe",
				"uptime":      99.8,
				"avgResponse": 250, // milliseconds
				"successRate": 98.5,
				"totalTx":     1205,
				"failedTx":    18,
				"fees":        4250.75,
			},
			{
				"name":        "PayPal",
				"uptime":      99.5,
				"avgResponse": 420,
				"successRate": 96.2,
				"totalTx":     823,
				"failedTx":    31,
				"fees":        2890.50,
			},
			{
				"name":        "Bank Transfer",
				"uptime":      99.9,
				"avgResponse": 1200,
				"successRate": 99.1,
				"totalTx":     445,
				"failedTx":    4,
				"fees":        120.00,
			},
		},
		"alerts": []Alert{
			{Type: "warning", Message: "Stripe response time above threshold", Time: "10 mins ago"},
			{Type: "info", Message: "PayPal uptime restored to normal", Time: "2 hours ago"},
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(performance)
}

// GetCustomerAnalytics returns customer-related analytics
func (h *AnalyticsHandler) GetCustomerAnalytics(w http.ResponseWriter, r *http.Request) {
	// Mock customer analytics (in real implementation, aggregate from payments)
	analytics := map[string]interface{}{
		"totalCustomers": 2845,
		"newCustomers":   156,
		"returningRate":  67.8,
		"avgOrderValue":  127.50,
		"topSpenders": []map[string]interface{}{
			{"customer": "john.doe@example.com", "totalSpent": 2450.75, "orders": 18},
			{"customer": "jane.smith@example.com", "totalSpent": 1890.25, "orders": 12},
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(analytics)
}

// GetDailyReport returns daily payment report
func (h *AnalyticsHandler) GetDailyReport(w http.ResponseWriter, r *http.Request) {
	date := r.URL.Query().Get("date")
	if date == "" {
		date = time.Now().Format("2006-01-02")
	}

	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	report := h.generateDailyReport(payments, date)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(report)
}

// GetWeeklyReport returns weekly payment report
func (h *AnalyticsHandler) GetWeeklyReport(w http.ResponseWriter, r *http.Request) {
	weekStr := r.URL.Query().Get("week")

	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	report := h.generateWeeklyReport(payments, weekStr)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(report)
}

// GetMonthlyReport returns monthly payment report
func (h *AnalyticsHandler) GetMonthlyReport(w http.ResponseWriter, r *http.Request) {
	monthStr := r.URL.Query().Get("month")

	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	report := h.generateMonthlyReport(payments, monthStr)

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(report)
}

// ExportPayments exports payment data as CSV
func (h *AnalyticsHandler) ExportPayments(w http.ResponseWriter, r *http.Request) {
	format := r.URL.Query().Get("format")
	if format == "" {
		format = "csv"
	}

	dateFrom := r.URL.Query().Get("dateFrom")
	dateTo := r.URL.Query().Get("dateTo")

	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if format == "csv" {
		h.exportPaymentsCSV(w, payments, dateFrom, dateTo)
	} else {
		http.Error(w, "Unsupported format", http.StatusBadRequest)
	}
}

// ExportRevenue exports revenue data
func (h *AnalyticsHandler) ExportRevenue(w http.ResponseWriter, r *http.Request) {
	format := r.URL.Query().Get("format")
	if format == "" {
		format = "csv"
	}

	period := r.URL.Query().Get("period")
	if period == "" {
		period = "month"
	}

	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if format == "csv" {
		h.exportRevenueCSV(w, payments, period)
	} else {
		http.Error(w, "Unsupported format", http.StatusBadRequest)
	}
}

// Helper methods for calculations and data processing

func (h *AnalyticsHandler) calculateDashboardMetrics(payments []*models.Payment) DashboardMetrics {
	var metrics DashboardMetrics

	// Calculate basic counts and revenue
	totalRevenue := 0.0
	successfulCount := 0
	failedCount := 0
	pendingCount := 0
	refundCount := 0
	refundAmount := 0.0

	for _, payment := range payments {
		switch payment.Status {
		case models.Completed:
			successfulCount++
			totalRevenue += payment.Amount
		case models.Failed:
			failedCount++
		case models.Pending:
			pendingCount++
		case models.Refunded:
			refundCount++
			refundAmount += payment.Amount
		}
	}

	total := len(payments)
	var successRate float64
	if total > 0 {
		successRate = float64(successfulCount) / float64(total) * 100
	}

	// Set metrics
	metrics.Revenue.Today = totalRevenue // Simplified - should filter by date
	metrics.Revenue.Week = totalRevenue
	metrics.Revenue.Month = totalRevenue
	metrics.Revenue.Change = 12.5 // Mock change percentage

	metrics.Transactions.Total = total
	metrics.Transactions.Successful = successfulCount
	metrics.Transactions.Failed = failedCount
	metrics.Transactions.Pending = pendingCount
	metrics.Transactions.SuccessRate = successRate

	metrics.Refunds.Count = refundCount
	metrics.Refunds.Amount = refundAmount
	if total > 0 {
		metrics.Refunds.Rate = float64(refundCount) / float64(total) * 100
	}

	metrics.Invoices.Generated = successfulCount // Assuming invoice per successful payment
	metrics.Invoices.Sent = successfulCount
	metrics.Invoices.Paid = successfulCount

	// Mock alerts
	metrics.Alerts = []Alert{
		{Type: "warning", Message: "High failure rate detected", Time: "10 mins ago"},
		{Type: "info", Message: "Daily revenue target exceeded", Time: "1 hour ago"},
	}

	return metrics
}

func (h *AnalyticsHandler) filterPaymentsByPeriod(payments []*models.Payment, period string) []*models.Payment {
	// Simplified period filtering - in real implementation, use proper date filtering
	now := time.Now()
	var startTime time.Time

	switch period {
	case "today":
		startTime = time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, now.Location())
	case "week":
		startTime = now.AddDate(0, 0, -7)
	case "month":
		startTime = now.AddDate(0, -1, 0)
	default:
		return payments
	}

	var filtered []*models.Payment
	for _, payment := range payments {
		if payment.CreatedAt.After(startTime) {
			filtered = append(filtered, payment)
		}
	}
	return filtered
}

func (h *AnalyticsHandler) calculateRevenueAnalytics(payments []*models.Payment, period, groupBy string) map[string]interface{} {
	// Simplified revenue calculation
	revenueByDay := make(map[string]float64)

	for _, payment := range payments {
		if payment.Status == models.Completed {
			day := payment.CreatedAt.Format("2006-01-02")
			revenueByDay[day] += payment.Amount
		}
	}

	return map[string]interface{}{
		"period":       period,
		"groupBy":      groupBy,
		"revenueData":  revenueByDay,
		"totalRevenue": h.calculateTotalRevenue(payments),
	}
}

func (h *AnalyticsHandler) calculateTotalRevenue(payments []*models.Payment) float64 {
	total := 0.0
	for _, payment := range payments {
		if payment.Status == models.Completed {
			total += payment.Amount
		}
	}
	return total
}

func (h *AnalyticsHandler) calculateTransactionAnalytics(payments []*models.Payment, period string) map[string]interface{} {
	filtered := h.filterPaymentsByPeriod(payments, period)

	return map[string]interface{}{
		"period":       period,
		"totalCount":   len(filtered),
		"successCount": h.countByStatus(filtered, models.Completed),
		"failedCount":  h.countByStatus(filtered, models.Failed),
		"pendingCount": h.countByStatus(filtered, models.Pending),
		"refundCount":  h.countByStatus(filtered, models.Refunded),
		"avgAmount":    h.calculateAverageAmount(filtered),
	}
}

func (h *AnalyticsHandler) calculateSuccessRates(payments []*models.Payment) map[string]interface{} {
	// Calculate success rates by payment method
	methodStats := make(map[models.PaymentMethod]map[string]int)

	for _, payment := range payments {
		if methodStats[payment.Method] == nil {
			methodStats[payment.Method] = make(map[string]int)
		}

		methodStats[payment.Method]["total"]++
		if payment.Status == models.Completed {
			methodStats[payment.Method]["successful"]++
		}
	}

	rates := make(map[string]interface{})
	for method, stats := range methodStats {
		if stats["total"] > 0 {
			rate := float64(stats["successful"]) / float64(stats["total"]) * 100
			rates[string(method)] = map[string]interface{}{
				"total":       stats["total"],
				"successful":  stats["successful"],
				"successRate": rate,
			}
		}
	}

	return rates
}

func (h *AnalyticsHandler) calculatePaymentMethodAnalytics(payments []*models.Payment) map[string]interface{} {
	methodCounts := make(map[models.PaymentMethod]int)
	methodRevenue := make(map[models.PaymentMethod]float64)

	for _, payment := range payments {
		methodCounts[payment.Method]++
		if payment.Status == models.Completed {
			methodRevenue[payment.Method] += payment.Amount
		}
	}

	analytics := make(map[string]interface{})
	for method, count := range methodCounts {
		analytics[string(method)] = map[string]interface{}{
			"count":   count,
			"revenue": methodRevenue[method],
		}
	}

	return analytics
}

func (h *AnalyticsHandler) countByStatus(payments []*models.Payment, status models.PaymentStatus) int {
	count := 0
	for _, payment := range payments {
		if payment.Status == status {
			count++
		}
	}
	return count
}

func (h *AnalyticsHandler) calculateAverageAmount(payments []*models.Payment) float64 {
	if len(payments) == 0 {
		return 0
	}

	total := 0.0
	for _, payment := range payments {
		total += payment.Amount
	}
	return total / float64(len(payments))
}

func (h *AnalyticsHandler) generateDailyReport(payments []*models.Payment, date string) map[string]interface{} {
	// Filter payments by date and generate report
	// Simplified implementation
	return map[string]interface{}{
		"date":         date,
		"totalRevenue": h.calculateTotalRevenue(payments),
		"totalCount":   len(payments),
		"avgAmount":    h.calculateAverageAmount(payments),
	}
}

func (h *AnalyticsHandler) generateWeeklyReport(payments []*models.Payment, week string) map[string]interface{} {
	return map[string]interface{}{
		"week":         week,
		"totalRevenue": h.calculateTotalRevenue(payments),
		"totalCount":   len(payments),
	}
}

func (h *AnalyticsHandler) generateMonthlyReport(payments []*models.Payment, month string) map[string]interface{} {
	return map[string]interface{}{
		"month":        month,
		"totalRevenue": h.calculateTotalRevenue(payments),
		"totalCount":   len(payments),
	}
}

func (h *AnalyticsHandler) exportPaymentsCSV(w http.ResponseWriter, payments []*models.Payment, dateFrom, dateTo string) {
	w.Header().Set("Content-Type", "text/csv")
	w.Header().Set("Content-Disposition", "attachment; filename=payments.csv")

	// Write CSV header
	w.Write([]byte("ID,Order ID,Amount,Method,Status,Created At\n"))

	// Write payment data
	for _, payment := range payments {
		line := payment.ID.String() + "," +
			payment.OrderID.String() + "," +
			strconv.FormatFloat(payment.Amount, 'f', 2, 64) + "," +
			string(payment.Method) + "," +
			string(payment.Status) + "," +
			payment.CreatedAt.Format("2006-01-02 15:04:05") + "\n"
		w.Write([]byte(line))
	}
}

func (h *AnalyticsHandler) exportRevenueCSV(w http.ResponseWriter, payments []*models.Payment, period string) {
	w.Header().Set("Content-Type", "text/csv")
	w.Header().Set("Content-Disposition", "attachment; filename=revenue.csv")

	// Write CSV header
	w.Write([]byte("Date,Revenue,Transaction Count\n"))

	// Group by date and write revenue data
	revenueByDay := make(map[string]float64)
	countByDay := make(map[string]int)

	for _, payment := range payments {
		if payment.Status == models.Completed {
			day := payment.CreatedAt.Format("2006-01-02")
			revenueByDay[day] += payment.Amount
			countByDay[day]++
		}
	}

	for date, revenue := range revenueByDay {
		line := date + "," +
			strconv.FormatFloat(revenue, 'f', 2, 64) + "," +
			strconv.Itoa(countByDay[date]) + "\n"
		w.Write([]byte(line))
	}
}
