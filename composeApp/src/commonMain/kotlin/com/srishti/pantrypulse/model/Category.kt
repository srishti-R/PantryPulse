/**
 * Solid Material 3 Multiplatform Enum mapping food categories with human-readable displayName strings
 * for elegant cross-platform database filtering.
 */
enum class Category(val displayName: String) {
    DAIRY("Dairy"),
    FRUITS_VEG("Fruits & Vegetables"),
    BAKERY("Bakery & Bread"),
    MEAT_SEAFOOD("Meat & Seafood"),
    PANTRY("Pantry Essentials"),
    BEVERAGES("Beverages"),
    FROZEN("Frozen Foods"),
    SNACKS("Snacks"),
    OTHER("Other"),
    NA("N/A");
}