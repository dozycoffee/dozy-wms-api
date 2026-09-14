INSERT INTO common_code (code, group_code, name, sort_order, active, created_at, created_by, updated_at, updated_by)
VALUES
    -- PRODUCT_CATEGORY: 상품 분류
    ('PRODUCT_CATEGORY_BEAN',  'PRODUCT_CATEGORY', '원두',          1, 1, NOW(6), 'system', NOW(6), 'system'),
    ('PRODUCT_CATEGORY_SYRUP', 'PRODUCT_CATEGORY', '시럽',          2, 1, NOW(6), 'system', NOW(6), 'system'),
    ('PRODUCT_CATEGORY_POWDER','PRODUCT_CATEGORY', '분말/파우더',    3, 1, NOW(6), 'system', NOW(6), 'system'),
    ('PRODUCT_CATEGORY_DAIRY', 'PRODUCT_CATEGORY', '유제품',        4, 1, NOW(6), 'system', NOW(6), 'system'),
    ('PRODUCT_CATEGORY_SUPPLY','PRODUCT_CATEGORY', '컵/소모품/포장재', 5, 1, NOW(6), 'system', NOW(6), 'system');
