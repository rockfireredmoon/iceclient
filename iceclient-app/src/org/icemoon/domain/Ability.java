package org.icemoon.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class Ability {
	public enum AbilityClass {
		Buff, Cast, Charge, Damage, Debuff, Detaunt, Execute, Healing, None, Passive, Taunt, Travel, Use, Visual;
		public static AbilityClass fromString(String str) {
			if (str.equals("")) {
				return None;
			} else if (str.equals("cast")) {
				return Cast;
			} else
				return AbilityClass.valueOf(str);
		}

		public String toClassString() {
			switch (this) {
			case None:
				return "";
			default:
				return name();
			}
		}
	}

	public enum BuffType {
		Buff, Debuff, None, World;
		public static BuffType fromString(String t) {
			if (t.startsWith("w/")) {
				return BuffType.World;
			} else if (t.startsWith("+/")) {
				return BuffType.Buff;
			} else if (t.startsWith("-/")) {
				return BuffType.Debuff;
			}
			return BuffType.None;
		}

		public String toChar() {
			switch (this) {
			case World:
				return "w";
			case Buff:
				return "+";
			case Debuff:
				return "-";
			default:
				return "";
			}
		}
	}

	public enum TargetStatus {
		Dead, Enemy, Enemy_Alive, Friend, Friend_Alive, Friend_Dead, None;
		public static TargetStatus fromString(String str) {
			return str.trim().equals("") ? None : TargetStatus.valueOf(str.trim().replace(" ", "_"));
		}

		public String toTargetString() {
			return this == None ? "" : name().replace("_", " ");
		}
	}

	private AbilityClass abilityClass = AbilityClass.None;
	private int abilityId;
	private String activationActions;
	private String activationCriteria;
	private int addMagicCharge;
	private int addMeleeCharge;
	private boolean allowDeadState;
	private String buffCategory;
	private String buffTitle;
	private BuffType buffType = BuffType.None;
	private String category;
	private int classCost;
	private String cooldownCategory;
	private long cooldownTime;
	private int crossCost;
	private String description;
	private boolean druid;
	private long duration;
	private long goldCost;
	private int groupId;
	private int hostility;
	private String icon1;
	private String icon2;
	private long interval;
	private boolean knight;
	private int level;
	private boolean mage;
	private String name;
	private int ownage;
	private List<Integer> requiredAbilities = new ArrayList<Integer>();
	private boolean rogue;
	private boolean secondaryChannel;
	private TargetStatus targetStatus = TargetStatus.Friend;
	private int tier;
	private boolean unbreakableChannel;
	private int useType;
	private String visualCue;
	private String warmupCue;
	private long warmupTime;
	private int x;
	private int y;

	// "-/DBMove/Debuff:Attack Speed" "Enemy Alive"/
	public Ability() {
	}

	public final AbilityClass getAbilityClass() {
		return abilityClass;
	}

	public int getAbilityId() {
		return abilityId;
	}

	public final String getActivationActions() {
		return activationActions;
	}

	public final String getActivationCriteria() {
		return activationCriteria;
	}

	public final int getAddMagicCharge() {
		return addMagicCharge;
	}

	public final int getAddMeleeCharge() {
		return addMeleeCharge;
	}

	public final String getBuffCategory() {
		return buffCategory;
	}

	public final String getBuffTitle() {
		return buffTitle;
	}

	public final BuffType getBuffType() {
		return buffType;
	}

	public final String getCategory() {
		return category;
	}

	public final int getClassCost() {
		return classCost;
	}

	public final String getCooldownCategory() {
		return cooldownCategory;
	}

	public final long getCooldownTime() {
		return cooldownTime;
	}

	public final int getCrossCost() {
		return crossCost;
	}

	public final String getDescription() {
		return description;
	}

	public final long getDuration() {
		return duration;
	}

	public final long getGoldCost() {
		return goldCost;
	}

	public final int getGroupId() {
		return groupId;
	}

	public final int getHostility() {
		return hostility;
	}

	public final String getIcon1() {
		return icon1;
	}

	public final String getIcon2() {
		return icon2;
	}

	public final long getInterval() {
		return interval;
	}

	public final int getLevel() {
		return level;
	}

	public final String getName() {
		return name;
	}

	public final int getOwnage() {
		return ownage;
	}

	public final List<Integer> getRequiredAbilities() {
		return requiredAbilities;
	}

	public final TargetStatus getTargetStatus() {
		return targetStatus;
	}

	public final int getTier() {
		return tier;
	}

	public final int getUseType() {
		return useType;
	}

	public final String getVisualCue() {
		return visualCue;
	}

	public final String getWarmupCue() {
		return warmupCue;
	}

	public final long getWarmupTime() {
		return warmupTime;
	}

	public final int getX() {
		return x;
	}

	public final int getY() {
		return y;
	}

	public final boolean isAllowDeadState() {
		return allowDeadState;
	}

	public boolean isAnyClass() {
		return mage || knight || druid || rogue;
	}

	public final boolean isDruid() {
		return druid;
	}

	public final boolean isKnight() {
		return knight;
	}

	public final boolean isMage() {
		return mage;
	}

	public final boolean isRogue() {
		return rogue;
	}

	public final boolean isSecondaryChannel() {
		return secondaryChannel;
	}

	public final boolean isUnbreakableChannel() {
		return unbreakableChannel;
	}

	public void resetClasses() {
		mage = false;
		druid = false;
		knight = false;
		rogue = false;
	}

	public void set(String[] row, String comment) {
		List<String> b = new ArrayList<String>(Arrays.asList(row));
		abilityId = Integer.parseInt(b.get(0));
		setName(b.get(1));
		setHostility(Integer.parseInt(b.get(2)));
		setWarmupTime(Long.parseLong(b.get(3)));
		setWarmupCue(b.get(4));
		setDuration(Long.parseLong(b.get(5)));
		setInterval(Long.parseLong(b.get(6)));
		setCooldownCategory(b.get(7));
		setCooldownTime(Long.parseLong(b.get(8)));
		setActivationCriteria(b.get(9));
		setActivationActions(b.get(10));
		setVisualCue(b.get(11));
		setTier(zeroIfBlank(b.get(12)));
		String[] prereq = b.get(13).split(",");
		if (prereq.length > 0) {
			setLevel(zeroIfBlank(prereq[0]));
			if (prereq.length > 1) {
				setCrossCost(zeroIfBlank(prereq[1]));
				if (prereq.length > 2) {
					setClassCost(zeroIfBlank(prereq[2]));
					if (prereq.length > 3) {
						String abstr = prereq[3];
						while (abstr.startsWith("(")) {
							abstr = abstr.substring(1);
						}
						while (abstr.endsWith(")")) {
							abstr = abstr.substring(0, abstr.length() - 1);
						}
						if (abstr.length() > 0) {
							String[] abs = abstr.split("\\|");
							for (String ab : abs) {
								requiredAbilities.add(Integer.parseInt(ab));
							}
						}
						// Abs
						if (prereq.length > 4) {
							// Abs
							resetClasses();
							for (char c : prereq[4].toCharArray()) {
								switch (c) {
								case 'K':
									setKnight(true);
									break;
								case 'M':
									setMage(true);
									break;
								case 'D':
									setDruid(true);
									break;
								case 'R':
									setRogue(true);
									break;
								}
							}
						}
					}
				}
			}
		}
		String[] icn = b.get(14).split("\\|");
		if (icn.length > 0) {
			setIcon1(icn[0]);
			if (icn.length > 1) {
				setIcon2(icn[1]);
			}
		}
		setDescription(b.get(15));
		setCategory(b.get(16));
		setX(Integer.parseInt(b.get(17)));
		setY(Integer.parseInt(b.get(18)));
		setGroupId(Integer.parseInt(b.get(19)));
		setAbilityClass(AbilityClass.fromString(b.get(20)));
		setUseType(Integer.parseInt(b.get(21)));
		setAddMeleeCharge(Integer.parseInt(b.get(22)));
		setAddMagicCharge(Integer.parseInt(b.get(23)));
		setOwnage(Integer.parseInt(b.get(24)));
		setGoldCost(Integer.parseInt(b.get(25)));
		String buff = b.get(26);
		setBuffType(BuffType.fromString(buff));
		if (getBuffType() != BuffType.None) {
			setBuffCategory(buff.substring(2, buff.indexOf(':')));
			setBuffTitle(buff.substring(buff.indexOf(':') + 1));
		}
		setTargetStatus(TargetStatus.fromString(b.get(27)));
		if (b.size() > 28) {
			for (String s : b.get(28).split("\\|")) {
				if (s.equalsIgnoreCase("secondarychannel")) {
					setSecondaryChannel(true);
				} else if (s.equalsIgnoreCase("unbreakablechannel")) {
					setUnbreakableChannel(true);
				} else if (s.equalsIgnoreCase("allowdeadstate")) {
					setAllowDeadState(true);
				}
			}
		}
	}

	public final void setAbilityClass(AbilityClass abilityClass) {
		this.abilityClass = abilityClass;
	}

	public void setAbilityId(int abilityId) {
		this.abilityId = abilityId;
	}

	public final void setActivationActions(String activationActions) {
		this.activationActions = activationActions;
	}

	public final void setActivationCriteria(String activationCriteria) {
		this.activationCriteria = activationCriteria;
	}

	public final void setAddMagicCharge(int addMagicCharge) {
		this.addMagicCharge = addMagicCharge;
	}

	public final void setAddMeleeCharge(int addMeleeCharge) {
		this.addMeleeCharge = addMeleeCharge;
	}

	public final void setAllowDeadState(boolean allowDeadState) {
		this.allowDeadState = allowDeadState;
	}

	public final void setBuffCategory(String buffCategory) {
		this.buffCategory = buffCategory;
	}

	public final void setBuffTitle(String buffTitle) {
		this.buffTitle = buffTitle;
	}

	public final void setBuffType(BuffType buffType) {
		this.buffType = buffType;
	}

	public final void setCategory(String category) {
		this.category = category;
	}

	public final void setClassCost(int classCost) {
		this.classCost = classCost;
	}

	public final void setCooldownCategory(String cooldownCategory) {
		this.cooldownCategory = cooldownCategory;
	}

	public final void setCooldownTime(long cooldownTime) {
		this.cooldownTime = cooldownTime;
	}

	public final void setCrossCost(int crossCost) {
		this.crossCost = crossCost;
	}

	public final void setDescription(String description) {
		this.description = description;
	}

	public final void setDruid(boolean druid) {
		this.druid = druid;
	}

	public final void setDuration(long duration) {
		this.duration = duration;
	}

	public final void setGoldCost(long goldCost) {
		this.goldCost = goldCost;
	}

	public final void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public final void setHostility(int hostility) {
		this.hostility = hostility;
	}

	public final void setIcon1(String icon1) {
		this.icon1 = icon1;
	}

	public final void setIcon2(String icon2) {
		this.icon2 = icon2;
	}

	public final void setInterval(long interval) {
		this.interval = interval;
	}

	public final void setKnight(boolean knight) {
		this.knight = knight;
	}

	public final void setLevel(int level) {
		this.level = level;
	}

	public final void setMage(boolean mage) {
		this.mage = mage;
	}

	public final void setName(String name) {
		this.name = name;
	}

	public final void setOwnage(int ownage) {
		this.ownage = ownage;
	}

	public final void setRequiredAbilities(List<Integer> requiredAbilities) {
		this.requiredAbilities = requiredAbilities;
	}

	public final void setRogue(boolean rogue) {
		this.rogue = rogue;
	}

	public final void setSecondaryChannel(boolean secondaryChannel) {
		this.secondaryChannel = secondaryChannel;
	}

	public final void setTargetStatus(TargetStatus targetStatus) {
		this.targetStatus = targetStatus;
	}

	public final void setTier(int tier) {
		this.tier = tier;
	}

	public final void setUnbreakableChannel(boolean unbreakableChannel) {
		this.unbreakableChannel = unbreakableChannel;
	}

	public final void setUseType(int useType) {
		this.useType = useType;
	}

	public final void setVisualCue(String visualCue) {
		this.visualCue = visualCue;
	}

	public final void setWarmupCue(String warmupCue) {
		this.warmupCue = warmupCue;
	}

	public final void setWarmupTime(long warmupTime) {
		this.warmupTime = warmupTime;
	}

	public final void setX(int x) {
		this.x = x;
	}

	public final void setY(int y) {
		this.y = y;
	}

	public String toString() {
		return getAbilityId() + " - " + getName();
	}

	private int zeroIfBlank(String n) {
		return n.trim().equals("") ? 0 : Integer.parseInt(n.trim());
	}

}
