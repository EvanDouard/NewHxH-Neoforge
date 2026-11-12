package com.evandouard.hxh.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

/**
 * Progression des quêtes d'un joueur : les quêtes en cours (plusieurs à la fois, chacune avec son étape et sa
 * progression), la quête suivie (affichée à l'écran), les quêtes terminées, le statut de Hunter, la Tour Céleste.
 * <p>
 * {@link #active}, {@link #step} et {@link #progress} forment un « curseur » sur une des quêtes en cours :
 * {@link #focus} le place, {@link #commit} enregistre ce qu'il contient. Au repos, il est sur la quête suivie.
 */
public final class QuestData implements INBTSerializable<CompoundTag> {
    String active = "";
    int step = 0;
    /** Mobs tués, ou ticks passés à suivre le PNJ, selon l'étape. */
    int progress = 0;
    /** Quêtes en cours : identifiant → {étape, progression}, dans l'ordre où elles ont commencé. */
    final java.util.LinkedHashMap<String, int[]> running = new java.util.LinkedHashMap<>();
    /** Quête suivie (affichée à l'écran), vide si aucune. */
    String tracked = "";
    final Set<String> completed = new HashSet<>();
    boolean hunter = false;
    /** D'où l'on est parti pour l'Examen (dimension et position), pour y revenir à la fin. */
    String examReturnDim = "";
    long examReturnPos = Long.MIN_VALUE;
    /** Examen Hunter raté : pas de nouvel essai avant ce moment (temps de jeu), ni dans le même centre. */
    long examRetryAt = 0;
    long examFailedCenter = Long.MIN_VALUE;
    /** Centre d'examen (place du Navigateur) de l'examen en cours. */
    long examCenter = Long.MIN_VALUE;
    /** Quêtes à épreuves tirées au sort : identifiant → indices des étapes du parcours tiré. */
    final java.util.Map<String, int[]> plans = new java.util.HashMap<>();
    /** Épreuves du parcours tiré (dans l'ordre), pour l'affichage. */
    final java.util.Map<String, java.util.List<String>> planTrials = new java.util.HashMap<>();
    /** Lieux de l'Examen déjà donnés à ce joueur : identifiant du lieu → nombre d'exemplaires utilisés. */
    final java.util.Map<String, Integer> examUses = new java.util.HashMap<>();
    /** Exemplaire de chaque lieu pour le parcours en cours (un lieu passé, réussi ou raté, ne revient jamais). */
    final java.util.Map<String, Integer> examInstances = new java.util.HashMap<>();
    /** Point de départ de l'Examen donné par le guide (place du passeur dans le monde), ou Long.MIN_VALUE. */
    long examDeparture = Long.MIN_VALUE;

    // ---------- Tour Céleste ----------
    boolean arenaRegistered = false;
    int arenaFloor = 1;
    /** Dernière tranche d'étages où l'hôtesse a inscrit le joueur (−1 = aucune). */
    int registeredBracket = -1;
    /** Inscrit au 200e étage (il faut le Nen). */
    boolean registered200 = false;
    /** Organisation rejointe (Brigade Fantôme, Association…), vide si aucune. */
    String organization = "";
    /** Famille (Zoldyck, Kurta…), vide si aucune ; tirée une seule fois. */
    String family = "";
    boolean familyAssigned = false;
    int arenaWins = 0;
    int arenaLosses = 0;
    int jenny = 0;
    /** Où renvoyer le joueur quand il quitte la Tour (dimension + position), vide = point d'apparition. */
    String returnDim = "";
    /** Inventaire mis de côté pendant un combat de la Tour (vide = rien à rendre). */
    net.minecraft.nbt.ListTag arenaStash = new net.minecraft.nbt.ListTag();
    double returnX, returnY, returnZ;

    /** Quête suivie (vide si aucune quête en cours). */
    public String active() {
        return active;
    }

    /** Quêtes en cours, dans l'ordre où elles ont commencé. */
    public java.util.List<String> runningQuests() {
        commit();
        return java.util.List.copyOf(running.keySet());
    }

    /** Place une quête en cours à une étape donnée (commandes et tests). */
    public void setQuestStep(String questId, int stepIndex) {
        commit();
        running.put(questId, new int[]{stepIndex, 0});
        if (questId.equals(active)) {
            step = stepIndex;
            progress = 0;
        }
    }

    /** {étape, progression} d'une quête en cours, ou null. */
    public int[] stateOf(String questId) {
        commit();
        int[] st = running.get(questId);
        return st == null ? null : st.clone();
    }

    public boolean isRunning(String questId) {
        return running.containsKey(questId);
    }

    public String tracked() {
        return tracked;
    }

    /** Suit une autre quête en cours ; renvoie false si elle n'est pas en cours. */
    public boolean track(String questId) {
        commit();
        if (!running.containsKey(questId)) return false;
        tracked = questId;
        focus(questId);
        return true;
    }

    /** Place le curseur sur une quête en cours. */
    void focus(String questId) {
        commit();
        int[] state = running.get(questId);
        active = state == null ? "" : questId;
        step = state == null ? 0 : state[0];
        progress = state == null ? 0 : state[1];
    }

    /** Enregistre le curseur dans la quête qu'il désigne (si elle est toujours en cours). */
    void commit() {
        if (!active.isEmpty() && running.containsKey(active)) running.put(active, new int[]{step, progress});
    }

    /** Remet le curseur sur la quête suivie (ou la première en cours si la quête suivie est finie). */
    void refocus() {
        commit();
        if (!running.containsKey(tracked)) tracked = running.isEmpty() ? "" : running.keySet().iterator().next();
        focus(tracked);
    }

    public int step() {
        return step;
    }

    public void setProgress(int value) {
        progress = value;
    }

    public int progress() {
        return progress;
    }

    public boolean isCompleted(String questId) {
        return completed.contains(questId);
    }

    /** Indices des étapes du parcours tiré pour cette quête, ou null (parcours fixe). */
    public int[] plan(String questId) {
        return plans.get(questId);
    }

    public java.util.List<String> planTrials(String questId) {
        return planTrials.getOrDefault(questId, java.util.List.of());
    }

    /** Exemplaire du lieu pour le parcours en cours (0 si le lieu n'en fait pas partie). */
    public int examInstance(String siteId) {
        return examInstances.getOrDefault(siteId, 0);
    }

    public long examDeparture() {
        return examDeparture;
    }

    public void setExamDeparture(long pos) {
        examDeparture = pos;
    }

    public int examUses(String siteId) {
        return examUses.getOrDefault(siteId, 0);
    }

    /** Nouveau parcours : chaque lieu reçoit le premier exemplaire que ce joueur n'a pas encore visité. */
    void setPlan(String questId, java.util.List<String> trials, int[] steps, java.util.Collection<String> sites) {
        plans.put(questId, steps);
        planTrials.put(questId, java.util.List.copyOf(trials));
        examInstances.clear();
        for (String site : sites) examInstances.put(site, examUses.getOrDefault(site, 0));
    }

    /** Le joueur est allé dans cet exemplaire du lieu : il ne le reverra plus (réussite ou échec). */
    public void markExamVisited(String siteId, int instance) {
        examUses.merge(siteId, instance + 1, Math::max);
    }

    public long examRetryAt() {
        return examRetryAt;
    }

    /** Pour l'administration et les tests : moment du prochain essai possible à l'examen. */
    public void setExamRetryAt(long gameTime) {
        examRetryAt = gameTime;
    }

    public long examFailedCenter() {
        return examFailedCenter;
    }

    public void setExamReturn(String dimension, long pos) {
        examReturnDim = dimension;
        examReturnPos = pos;
    }

    public String examReturnDimension() {
        return examReturnDim;
    }

    public long examReturnPos() {
        return examReturnPos;
    }

    public boolean isHunter() {
        return hunter;
    }

    public void setHunter(boolean value) {
        hunter = value;
    }

    public boolean arenaRegistered() {
        return arenaRegistered;
    }

    public void setArenaRegistered(boolean value) {
        arenaRegistered = value;
    }

    public int arenaFloor() {
        return arenaFloor;
    }

    public void setArenaFloor(int floor) {
        arenaFloor = Math.max(1, Math.min(com.evandouard.hxh.arena.ArenaRules.TOP_FLOOR, floor));
    }

    public int registeredBracket() {
        return registeredBracket;
    }

    public void setRegisteredBracket(int value) {
        registeredBracket = value;
    }

    public String family() {
        return family;
    }

    public void setFamily(String id) {
        family = id == null ? "" : id;
    }

    public boolean familyAssigned() {
        return familyAssigned;
    }

    public void setFamilyAssigned(boolean value) {
        familyAssigned = value;
    }

    public String organization() {
        return organization;
    }

    public void setOrganization(String id) {
        organization = id == null ? "" : id;
    }

    public boolean registered200() {
        return registered200;
    }

    public void setRegistered200(boolean value) {
        registered200 = value;
    }

    public int arenaWins() {
        return arenaWins;
    }

    public int arenaLosses() {
        return arenaLosses;
    }

    public int jenny() {
        return jenny;
    }

    public void addJenny(int amount) {
        jenny = Math.max(0, jenny + amount);
    }

    /** Enregistre un combat de la Tour : bilan, nouvel étage et Jenny gagnés. */
    public void recordArenaResult(boolean win, int newFloor, int jennyGained) {
        if (win) arenaWins++;
        else arenaLosses++;
        setArenaFloor(newFloor);
        addJenny(jennyGained);
    }

    public net.minecraft.nbt.ListTag arenaStash() {
        return arenaStash;
    }

    public void setArenaStash(net.minecraft.nbt.ListTag stash) {
        arenaStash = stash;
    }

    public void setReturnPoint(String dimension, double x, double y, double z) {
        returnDim = dimension;
        returnX = x;
        returnY = y;
        returnZ = z;
    }

    public String returnDim() {
        return returnDim;
    }

    public net.minecraft.world.phys.Vec3 returnPos() {
        return new net.minecraft.world.phys.Vec3(returnX, returnY, returnZ);
    }

    public void reset() {
        running.clear();
        tracked = "";
        active = "";
        step = 0;
        progress = 0;
        completed.clear();
        hunter = false;
        examRetryAt = 0;
        examFailedCenter = Long.MIN_VALUE;
        examCenter = Long.MIN_VALUE;
        plans.clear();
        planTrials.clear();
        examInstances.clear();
        examDeparture = Long.MIN_VALUE;
        arenaRegistered = false;
        arenaFloor = 1;
        registeredBracket = -1;
        registered200 = false;
        organization = "";
        arenaWins = 0;
        arenaLosses = 0;
        jenny = 0;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        commit();
        ListTag quests = new ListTag();
        running.forEach((id, st) -> {
            CompoundTag q = new CompoundTag();
            q.putString("id", id);
            q.putInt("step", st[0]);
            q.putInt("progress", st[1]);
            quests.add(q);
        });
        tag.put("running", quests);
        tag.putString("tracked", tracked);
        tag.putBoolean("hunter", hunter);
        if (!examReturnDim.isEmpty()) {
            tag.putString("exam_return_dim", examReturnDim);
            tag.putLong("exam_return_pos", examReturnPos);
        }
        tag.putLong("exam_retry_at", examRetryAt);
        tag.putLong("exam_failed_center", examFailedCenter);
        tag.putLong("exam_center", examCenter);
        CompoundTag planTag = new CompoundTag();
        plans.forEach((id, steps) -> {
            CompoundTag one = new CompoundTag();
            one.putIntArray("steps", steps);
            ListTag tr = new ListTag();
            for (String t : planTrials.getOrDefault(id, java.util.List.of())) tr.add(StringTag.valueOf(t));
            one.put("trials", tr);
            planTag.put(id, one);
        });
        tag.put("plans", planTag);
        CompoundTag uses = new CompoundTag();
        examUses.forEach(uses::putInt);
        tag.put("exam_uses", uses);
        CompoundTag inst = new CompoundTag();
        examInstances.forEach(inst::putInt);
        tag.put("exam_instances", inst);
        tag.putLong("exam_departure", examDeparture);
        tag.putString("organization", organization);
        tag.putString("family", family);
        tag.putBoolean("family_assigned", familyAssigned);
        ListTag done = new ListTag();
        for (String id : completed) done.add(StringTag.valueOf(id));
        tag.put("completed", done);
        CompoundTag arena = new CompoundTag();
        arena.putBoolean("registered", arenaRegistered);
        arena.putInt("floor", arenaFloor);
        arena.putInt("registered_bracket", registeredBracket);
        arena.putBoolean("registered_200", registered200);
        arena.putInt("wins", arenaWins);
        arena.putInt("losses", arenaLosses);
        arena.putInt("jenny", jenny);
        arena.putString("return_dim", returnDim);
        arena.put("stash", arenaStash);
        arena.putDouble("return_x", returnX);
        arena.putDouble("return_y", returnY);
        arena.putDouble("return_z", returnZ);
        tag.put("arena", arena);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        running.clear();
        for (Tag t : tag.getList("running", Tag.TAG_COMPOUND)) {
            CompoundTag q = (CompoundTag) t;
            running.put(q.getString("id"), new int[]{q.getInt("step"), q.getInt("progress")});
        }
        tracked = tag.getString("tracked");
        // Ancien format : une seule quête en cours
        String legacy = tag.getString("active");
        if (!legacy.isEmpty() && !running.containsKey(legacy)) {
            running.put(legacy, new int[]{tag.getInt("step"), tag.getInt("progress")});
            if (tracked.isEmpty()) tracked = legacy;
        }
        active = "";
        refocus();
        hunter = tag.getBoolean("hunter");
        examReturnDim = tag.getString("exam_return_dim");
        examReturnPos = tag.contains("exam_return_pos") ? tag.getLong("exam_return_pos") : Long.MIN_VALUE;
        examRetryAt = tag.getLong("exam_retry_at");
        examFailedCenter = tag.contains("exam_failed_center") ? tag.getLong("exam_failed_center") : Long.MIN_VALUE;
        examCenter = tag.contains("exam_center") ? tag.getLong("exam_center") : Long.MIN_VALUE;
        plans.clear();
        planTrials.clear();
        CompoundTag planTag = tag.getCompound("plans");
        for (String id : planTag.getAllKeys()) {
            CompoundTag one = planTag.getCompound(id);
            plans.put(id, one.getIntArray("steps"));
            java.util.List<String> tr = new java.util.ArrayList<>();
            for (Tag t : one.getList("trials", Tag.TAG_STRING)) tr.add(t.getAsString());
            planTrials.put(id, tr);
        }
        examUses.clear();
        CompoundTag uses = tag.getCompound("exam_uses");
        for (String k : uses.getAllKeys()) examUses.put(k, uses.getInt(k));
        examInstances.clear();
        CompoundTag inst = tag.getCompound("exam_instances");
        for (String k : inst.getAllKeys()) examInstances.put(k, inst.getInt(k));
        examDeparture = tag.contains("exam_departure") ? tag.getLong("exam_departure") : Long.MIN_VALUE;
        organization = tag.getString("organization");
        family = tag.getString("family");
        familyAssigned = tag.getBoolean("family_assigned");
        completed.clear();
        for (Tag t : tag.getList("completed", Tag.TAG_STRING)) completed.add(t.getAsString());
        CompoundTag arena = tag.getCompound("arena");
        arenaRegistered = arena.getBoolean("registered");
        arenaFloor = Math.max(1, arena.getInt("floor"));
        registeredBracket = arena.contains("registered_bracket") ? arena.getInt("registered_bracket") : (arenaRegistered ? 0 : -1);
        registered200 = arena.getBoolean("registered_200");
        arenaWins = arena.getInt("wins");
        arenaLosses = arena.getInt("losses");
        jenny = arena.getInt("jenny");
        returnDim = arena.getString("return_dim");
        arenaStash = arena.getList("stash", Tag.TAG_COMPOUND);
        returnX = arena.getDouble("return_x");
        returnY = arena.getDouble("return_y");
        returnZ = arena.getDouble("return_z");
    }
}
