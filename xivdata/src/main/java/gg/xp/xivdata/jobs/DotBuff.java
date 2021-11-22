package gg.xp.xivdata.jobs;

import java.util.Set;

import static gg.xp.xivdata.jobs.Job.AST;
import static gg.xp.xivdata.jobs.Job.BLM;
import static gg.xp.xivdata.jobs.Job.BLU;
import static gg.xp.xivdata.jobs.Job.BRD;
import static gg.xp.xivdata.jobs.Job.DRG;
import static gg.xp.xivdata.jobs.Job.MNK;
import static gg.xp.xivdata.jobs.Job.NIN;
import static gg.xp.xivdata.jobs.Job.PLD;
import static gg.xp.xivdata.jobs.Job.SAM;
import static gg.xp.xivdata.jobs.Job.SCH;
import static gg.xp.xivdata.jobs.Job.SMN;
import static gg.xp.xivdata.jobs.Job.WHM;

public enum DotBuff {
	// List of ALL buffs to track - WL/BL will be done by user settings
	// JLS/javac being dumb, had to put the L there to make it a long
	AST_Combust(AST, "Combust/II/III", 0x346L, 0x34bL, 0x759L),
	BLM_Thunder(BLM, "Thunder/II/III/IV", 0xa1L, 0xa2L, 0xa3L, 0x4baL),
	BLU_Bleeding(BLU, "Bleeding", 0x6b2L),
	BRD_CombinedDots(BRD, "Bard DoTs", 0x4b0L, 0x4b1L, 0x7cL, 0x81L),
	DRG_ChaosThrust(DRG, "Chaos Thrust", 0x76L),
	MNK_Demolish(MNK, "Demolish", 0xf6L),
	NIN_ShadowFang(NIN, "Shadow Fang", 0x1fcL),
	PLD_GoringBlade(PLD, "Goring Blade", 0x2d5L),
	SAM_Higanbana(SAM, "Higanbana", 0x4ccL),
	SCH_Bio(SCH, "Bio/II/Biolysis", 0xb3L, 0xbdL, 0x767L),
	SMN_Bio(SMN, "Bio/II/III", 0xb3L, 0xbdL, 0x4beL),
	SMN_Miasma(SMN, "Miasma/II/III", 0xb4L, 0xbcL, 0x4bfL),
	WHM_Aero(WHM, "Aero/II/III/Dia", 0x8fL, 0x90L, 0x31eL, 0x74fL);

	private final Job job;
	private final String label;
	private final Set<Long> buffIds;

	DotBuff(Job job, String label, Long... buffIds) {
		this.job = job;
		this.label = label;
		this.buffIds = Set.of(buffIds);
	}

	public Job getJob() {
		return job;
	}

	public String getLabel() {
		return label;
	}


	public boolean matches(long id) {
		return buffIds.contains(id);
	}
}
