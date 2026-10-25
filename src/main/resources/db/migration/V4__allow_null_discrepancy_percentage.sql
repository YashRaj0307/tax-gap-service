
ALTER TABLE tax_gap_analysis
    DROP CONSTRAINT chk_analysis_percentage;

ALTER TABLE tax_gap_analysis
    ALTER COLUMN discrepancy_percentage DROP NOT NULL;

ALTER TABLE tax_gap_analysis
    ADD CONSTRAINT chk_analysis_percentage
    CHECK (
        discrepancy_percentage IS NULL
        OR discrepancy_percentage >= 0
    );
