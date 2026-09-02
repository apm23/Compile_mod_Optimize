package com.apm23.custompickaxe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class MultiPageInventoryCompatTest {
    private static final MultiPageInventoryCompat.SlotOps<FakeStack> OPS = new MultiPageInventoryCompat.SlotOps<>() {
        public boolean empty(FakeStack s) { return s == null || s.count == 0; }
        public boolean same(FakeStack a, FakeStack b) { return a.id.equals(b.id); }
        public int count(FakeStack s) { return s.count; }
        public int max(FakeStack s) { return s.max; }
        public void grow(FakeStack s, int n) { s.count += n; }
        public void shrink(FakeStack s, int n) { s.count -= n; }
        public FakeStack copyWithCount(FakeStack s, int n) { return new FakeStack(s.id, n, s.max); }
    };

    @Test void fillsExistingStacksBeforeEmptyHiddenSlotsWithoutLoss() {
        ArrayList<FakeStack> page = emptyPage(27); page.set(0, new FakeStack("diamond", 60, 64));
        FakeStack remaining = new FakeStack("diamond", 70, 64); MultiPageInventoryCompat.insertGeneric(page, remaining, OPS);
        assertTrue(OPS.empty(remaining)); assertEquals(64, page.get(0).count); assertEquals(64, page.get(1).count); assertEquals(2, page.get(2).count); assertEquals(130, count(page, "diamond"));
    }
    @Test void fullPageLeavesExactRemainderForFollowingPages() {
        ArrayList<FakeStack> page = fullPage("stone", 27); FakeStack remaining = new FakeStack("diamond", 37, 64);
        MultiPageInventoryCompat.insertGeneric(page, remaining, OPS); assertEquals(37, remaining.count); assertEquals(0, count(page, "diamond"));
    }
    @Test void differentItemsAreNeverMerged() {
        ArrayList<FakeStack> page = emptyPage(27); page.set(0, new FakeStack("emerald", 10, 64)); FakeStack remaining = new FakeStack("diamond", 5, 64);
        MultiPageInventoryCompat.insertGeneric(page, remaining, OPS); assertTrue(OPS.empty(remaining)); assertEquals(10, page.get(0).count); assertEquals(5, page.get(1).count);
    }
    @Test void fullActivePageRoutesRewardThroughHiddenPagesBeforeAltHotbarOrDrop() {
        List<ArrayList<FakeStack>> hiddenPages = new ArrayList<>(); hiddenPages.add(fullPage("stone", 27)); hiddenPages.add(emptyPage(27)); for (int i=0;i<5;i++) hiddenPages.add(emptyPage(27));
        ArrayList<FakeStack> altHotbar = emptyPage(9); FakeStack remaining = new FakeStack("iron_block", 130, 64);
        for (ArrayList<FakeStack> page : hiddenPages) { MultiPageInventoryCompat.insertGeneric(page, remaining, OPS); if (OPS.empty(remaining)) break; }
        if (!OPS.empty(remaining)) MultiPageInventoryCompat.insertGeneric(altHotbar, remaining, OPS);
        assertTrue(OPS.empty(remaining)); assertEquals(130, count(hiddenPages.get(1), "iron_block")); assertEquals(0, count(altHotbar, "iron_block"));
    }
    @Test void allHiddenPagesFullUsesAltHotbarBeforeWorldDrop() {
        List<ArrayList<FakeStack>> hiddenPages = new ArrayList<>(); for (int i=0;i<7;i++) hiddenPages.add(fullPage("stone", 27));
        ArrayList<FakeStack> altHotbar = emptyPage(9); FakeStack remaining = new FakeStack("debris", 65, 64);
        for (ArrayList<FakeStack> page : hiddenPages) MultiPageInventoryCompat.insertGeneric(page, remaining, OPS); MultiPageInventoryCompat.insertGeneric(altHotbar, remaining, OPS);
        assertTrue(OPS.empty(remaining)); assertEquals(65, count(altHotbar, "debris"));
    }
    private static ArrayList<FakeStack> emptyPage(int size) { ArrayList<FakeStack> page = new ArrayList<>(size); for (int i=0;i<size;i++) page.add(new FakeStack("",0,64)); return page; }
    private static ArrayList<FakeStack> fullPage(String id,int size) { ArrayList<FakeStack> page = new ArrayList<>(size); for(int i=0;i<size;i++) page.add(new FakeStack(id,64,64)); return page; }
    private static int count(List<FakeStack> stacks,String id) { return stacks.stream().filter(s -> s != null && s.count > 0 && s.id.equals(id)).mapToInt(s -> s.count).sum(); }
    private static final class FakeStack { private final String id; private int count; private final int max; private FakeStack(String id,int count,int max){this.id=id;this.count=count;this.max=max;} }
}
