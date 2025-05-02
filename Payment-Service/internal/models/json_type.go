package models

import (
	"database/sql/driver"
	"encoding/json"
	"errors"
)

// JSON custom type for PostgreSQL jsonb
type JSON map[string]interface{}

// Scan scans value into JSON, implements sql.Scanner interface
func (j *JSON) Scan(value interface{}) error {
	bytes, ok := value.([]byte)
	if !ok {
		return errors.New("type assertion to []byte failed")
	}
	return json.Unmarshal(bytes, &j)
}

// Value returns json value, implements driver.Valuer interface
func (j JSON) Value() (driver.Value, error) {
	if len(j) == 0 {
		return nil, nil
	}
	return json.Marshal(j)
}
