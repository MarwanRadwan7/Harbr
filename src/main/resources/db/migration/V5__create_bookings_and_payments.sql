CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    guest_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    guest_count INTEGER NOT NULL,
    base_amount DECIMAL(10,2) NOT NULL,
    cleaning_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    service_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_price DECIMAL(10,2) NOT NULL,
    status VARCHAR NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'REJECTED')) DEFAULT 'PENDING',
    cancellation_reason TEXT,
    cancelled_by VARCHAR,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT chk_check_out_after_check_in CHECK (check_out > check_in),
    CONSTRAINT chk_guest_count_positive CHECK (guest_count > 0),
    CONSTRAINT chk_total_price_positive CHECK (total_price >= 0)
);

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    provider VARCHAR NOT NULL CHECK (provider IN ('STRIPE', 'PAYPAL')),
    provider_tx_id VARCHAR NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REFUNDED')) DEFAULT 'PENDING',
    type VARCHAR NOT NULL CHECK (type IN ('CHARGE', 'REFUND')),
    failure_reason VARCHAR,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_bookings_property_id ON bookings(property_id);
CREATE INDEX idx_bookings_guest_id ON bookings(guest_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_dates ON bookings(property_id, check_in, check_out);
CREATE INDEX idx_bookings_created_at ON bookings(created_at);
CREATE INDEX idx_payment_transactions_booking_id ON payment_transactions(booking_id);
CREATE INDEX idx_payment_transactions_provider_tx_id ON payment_transactions(provider_tx_id);