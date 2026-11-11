package com.haebi.focuslock.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenCatalogTest {
    @Test fun savedItemNamesAndIdsRemainCompatible() {
        assertEquals("sprout", GardenCatalog.fromSaved(null).id)
        assertEquals("tree", GardenCatalog.fromSaved("작은 나무").id)
        assertEquals("tree", GardenCatalog.fromSaved("tree").id)
        assertEquals("sprout", GardenCatalog.fromSaved("unknown").id)
    }

    @Test fun lockedItemsCannotBePlacedAndMilestonesDoNotSpendPoints() {
        assertFalse(GardenCatalog.canPlace("tree", 149))
        assertTrue(GardenCatalog.canPlace("tree", 150))
        assertEquals("sprout", GardenCatalog.placed("tree", 149).id)
        assertEquals("tree", GardenCatalog.placed("tree", 150).id)
        assertEquals("sprout", GardenCatalog.placed("unknown", 1000).id)
    }
}
