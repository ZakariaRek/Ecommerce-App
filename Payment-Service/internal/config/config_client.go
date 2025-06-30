// Payment-Service/internal/config/config_client.go
package config

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

// ConfigServerResponse represents the response from Spring Cloud Config Server
type ConfigServerResponse struct {
	Name            string           `json:"name"`
	Profiles        []string         `json:"profiles"`
	Label           string           `json:"label"`
	Version         string           `json:"version"`
	State           string           `json:"state"`
	PropertySources []PropertySource `json:"propertySources"`
}

// PropertySource represents a property source from config server
type PropertySource struct {
	Name   string                 `json:"name"`
	Source map[string]interface{} `json:"source"`
}

// ConfigClient handles communication with Spring Cloud Config Server
type ConfigClient struct {
	ServerURL string
	AppName   string
	Profile   string
	Label     string
	Timeout   time.Duration
}

// NewConfigClient creates a new config client
func NewConfigClient(serverURL, appName, profile, label string) *ConfigClient {
	return &ConfigClient{
		ServerURL: serverURL,
		AppName:   appName,
		Profile:   profile,
		Label:     label,
		Timeout:   10 * time.Second,
	}
}

// FetchConfig fetches configuration from the config server
func (c *ConfigClient) FetchConfig() (*ConfigServerResponse, error) {
	url := fmt.Sprintf("%s/%s/%s/%s", c.ServerURL, c.AppName, c.Profile, c.Label)

	client := &http.Client{
		Timeout: c.Timeout,
	}

	resp, err := client.Get(url)
	if err != nil {
		return nil, fmt.Errorf("failed to fetch config from %s: %v", url, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("config server returned status %d: %s", resp.StatusCode, string(body))
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %v", err)
	}

	var configResponse ConfigServerResponse
	if err := json.Unmarshal(body, &configResponse); err != nil {
		return nil, fmt.Errorf("failed to unmarshal config response: %v", err)
	}

	return &configResponse, nil
}

// GetProperty retrieves a specific property from the config response
func (c *ConfigServerResponse) GetProperty(key string) (interface{}, bool) {
	// Iterate through property sources in reverse order (highest priority first)
	for i := len(c.PropertySources) - 1; i >= 0; i-- {
		if value, exists := c.PropertySources[i].Source[key]; exists {
			return value, true
		}
	}
	return nil, false
}

// GetStringProperty retrieves a string property with fallback
func (c *ConfigServerResponse) GetStringProperty(key, fallback string) string {
	if value, exists := c.GetProperty(key); exists {
		if str, ok := value.(string); ok {
			return str
		}
	}
	return fallback
}

// GetIntProperty retrieves an integer property with fallback
func (c *ConfigServerResponse) GetIntProperty(key string, fallback int) int {
	if value, exists := c.GetProperty(key); exists {
		switch v := value.(type) {
		case int:
			return v
		case float64:
			return int(v)
		case string:
			// Try to parse string as int if needed
			if parsed, err := json.Number(v).Int64(); err == nil {
				return int(parsed)
			}
		}
	}
	return fallback
}

// GetBoolProperty retrieves a boolean property with fallback
func (c *ConfigServerResponse) GetBoolProperty(key string, fallback bool) bool {
	if value, exists := c.GetProperty(key); exists {
		if b, ok := value.(bool); ok {
			return b
		}
		// Handle string representations of boolean
		if str, ok := value.(string); ok {
			return str == "true" || str == "1" || str == "yes"
		}
	}
	return fallback
}

// ToMap converts all properties to a flat map
func (c *ConfigServerResponse) ToMap() map[string]interface{} {
	result := make(map[string]interface{})

	// Merge all property sources (later sources override earlier ones)
	for _, ps := range c.PropertySources {
		for key, value := range ps.Source {
			result[key] = value
		}
	}

	return result
}
