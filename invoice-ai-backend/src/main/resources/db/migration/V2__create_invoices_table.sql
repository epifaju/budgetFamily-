CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    merchant VARCHAR(255),
    date DATE NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2),
    tax DECIMAL(10,2),
    payment_method VARCHAR(50),
    image_url TEXT,
    confidence_score DECIMAL(3,2),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoices_user_date ON invoices(user_id, date DESC);



