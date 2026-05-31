CREATE TABLE IF NOT EXISTS items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID REFERENCES invoices(id) ON DELETE CASCADE,
    name VARCHAR(500) NOT NULL,
    quantity DECIMAL(10,3) DEFAULT 1,
    unit_price DECIMAL(10,2),
    total_price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    confidence_score DECIMAL(3,2),
    is_corrected BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_items_invoice ON items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_items_category ON items(category);



