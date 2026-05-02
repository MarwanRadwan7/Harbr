CREATE TABLE addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    street VARCHAR NOT NULL,
    city VARCHAR NOT NULL,
    state VARCHAR,
    country VARCHAR NOT NULL,
    postal_code VARCHAR,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    address_id UUID NOT NULL REFERENCES addresses(id),
    title VARCHAR NOT NULL,
    description TEXT,
    property_type VARCHAR NOT NULL CHECK (property_type IN ('APARTMENT', 'HOUSE', 'VILLA', 'STUDIO', 'CABIN')),
    max_guests INTEGER NOT NULL,
    bedrooms INTEGER NOT NULL,
    bathrooms INTEGER NOT NULL,
    base_price_per_night DECIMAL(10,2) NOT NULL,
    cleaning_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    status VARCHAR NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING_REVIEW')) DEFAULT 'PENDING_REVIEW',
    is_instant_book BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE property_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    url VARCHAR NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_cover BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR NOT NULL UNIQUE,
    category VARCHAR NOT NULL CHECK (category IN ('BASIC', 'SAFETY', 'ACCESSIBILITY', 'OUTDOOR')),
    icon_key VARCHAR,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE TABLE property_amenities (
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    amenity_id UUID NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (property_id, amenity_id)
);

CREATE TABLE availability_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    rule_type VARCHAR NOT NULL CHECK (rule_type IN ('BLOCKED', 'PRICE_OVERRIDE', 'MIN_STAY')),
    price_override DECIMAL(10,2),
    min_stay_nights INTEGER,
    note VARCHAR,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_properties_host_id ON properties(host_id);
CREATE INDEX idx_properties_status ON properties(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_properties_created_at ON properties(created_at);
CREATE INDEX idx_property_images_property_id ON property_images(property_id);
CREATE INDEX idx_availability_rules_property_id ON availability_rules(property_id);
CREATE INDEX idx_addresses_city ON addresses(city);