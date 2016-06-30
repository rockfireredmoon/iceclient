__KeyMaps = __KeyMaps;

with (JavaImporter(org.icescene.io, com.jme3.input.controls, com.jme3.input)) {
	__KeyMaps.Options= {
		trigger : new KeyTrigger(KeyInput.KEY_O),
		category : "Other"
	};
	__KeyMaps.Console = {
		trigger : new KeyTrigger(KeyInput.KEY_GRAVE),
		category : "Other"
	};
	__KeyMaps.ConsoleOnce = {
		trigger : new KeyTrigger(KeyInput.KEY_SLASH),
		category : "Other"
	};
	__KeyMaps.HUD = {
		trigger : new KeyTrigger(KeyInput.KEY_Z),
		modifiers : ModifierKeysAppState.ALT_MASK,
		category : "Other"
	};
	__KeyMaps.Exit = {
		trigger : new KeyTrigger(KeyInput.KEY_ESCAPE),
		category : "Other"
	};
	__KeyMaps.Map = {
		trigger : new KeyTrigger(KeyInput.KEY_M),
		category : "Gameplay"
	};
	__KeyMaps.Inventory = {
		trigger : new KeyTrigger(KeyInput.KEY_I),
		category : "Gameplay"
	};
	__KeyMaps.Minimap = {
		trigger : new KeyTrigger(KeyInput.KEY_P),
		category : "Gameplay"
	};

	__KeyMaps.Arm = {
		trigger : new KeyTrigger(KeyInput.KEY_X),
		category : "Gameplay"
	};
	__KeyMaps.ToggleAutorun = {
		trigger : new KeyTrigger(KeyInput.KEY_Z),
		category : "Gameplay"
	};
	__KeyMaps.Forward = {
		trigger : new KeyTrigger(KeyInput.KEY_W),
		category : "Gameplay"
	};
	__KeyMaps.Forward1 = {
		trigger : new KeyTrigger(KeyInput.KEY_UP),
		category : "Gameplay"
	};
	__KeyMaps.Backward = {
		trigger : new KeyTrigger(KeyInput.KEY_S),
		category : "Gameplay"
	};
	__KeyMaps.Backward1 = {
		trigger : new KeyTrigger(KeyInput.KEY_DOWN),
		category : "Gameplay"
	};
	__KeyMaps.StrafeLeft = {
		trigger : new KeyTrigger(KeyInput.KEY_Q),
		category : "Gameplay"
	};
	__KeyMaps.StrafeRight = {
		trigger : new KeyTrigger(KeyInput.KEY_E),
		category : "Gameplay"
	};
	__KeyMaps.RotateLeft = {
		trigger : new KeyTrigger(KeyInput.KEY_A),
		category : "Gameplay"
	};
	__KeyMaps.RotateRight = {
		trigger : new KeyTrigger(KeyInput.KEY_D),
		category : "Gameplay"
	};
	__KeyMaps.RotateLeft1 = {
		trigger : new KeyTrigger(KeyInput.KEY_LEFT),
		category : "Gameplay"
	};
	__KeyMaps.RotateRight1 = {
		trigger : new KeyTrigger(KeyInput.KEY_RIGHT),
		category : "Gameplay"
	};
	__KeyMaps.Jump = {
		trigger : new KeyTrigger(KeyInput.KEY_SPACE),
		category : "Gameplay"
	};
	__KeyMaps.Abilities = {
		trigger : new KeyTrigger(KeyInput.KEY_B),
		category : "Gameplay"
	};
	__KeyMaps.ToggleChat = {
		trigger : new KeyTrigger(KeyInput.KEY_C),
		category : "Gameplay",
		modifiers : ModifierKeysAppState.ALT_MASK
	};
	__KeyMaps.MouseLeft = {
		trigger : new MouseButtonTrigger(MouseInput.BUTTON_LEFT),
		category : "Gameplay"
	};
	__KeyMaps.MouseRight = {
		trigger : new MouseButtonTrigger(MouseInput.BUTTON_RIGHT),
		category : "Gameplay"
	};
	__KeyMaps.Character = {
		trigger : new KeyTrigger(KeyInput.KEY_C),
		category : "Gameplay"
	};
	__KeyMaps.EnvironmentEdit = {
		trigger : new KeyTrigger(KeyInput.KEY_E),
		category : "Editing",
		modifiers : ModifierKeysAppState.ALT_MASK
	};
	__KeyMaps.Inventory = {
		trigger : new KeyTrigger(KeyInput.KEY_I),
		category : "Gameplay"
	};
	__KeyMaps.Quests = {
		trigger : new KeyTrigger(KeyInput.KEY_J),
		category : "Gameplay"
	};
	__KeyMaps.Social = {
		trigger : new KeyTrigger(KeyInput.KEY_F),
		category : "Gameplay"
	};
	__KeyMaps.TerrainEdit = {
		trigger : new KeyTrigger(KeyInput.KEY_T),
		modifiers : ModifierKeysAppState.ALT_MASK,
		category : "Editing"
	};
	__KeyMaps.Undo = {
		trigger : new KeyTrigger(KeyInput.KEY_Z),
		modifiers : ModifierKeysAppState.CTRL_MASK,
		category : "Editing"
	};
	__KeyMaps.Redo = {
		trigger : new KeyTrigger(KeyInput.KEY_Y),
		modifiers : ModifierKeysAppState.CTRL_MASK,
		category : "Editing"
	};
	__KeyMaps.BuildMode = {
		trigger : new KeyTrigger(KeyInput.KEY_B),
		modifiers : ModifierKeysAppState.CTRL_MASK,
		category : "Editing"
	};
	__KeyMaps.CreatureTweak = {
		trigger : new KeyTrigger(KeyInput.KEY_T),
		modifiers : ModifierKeysAppState.CTRL_MASK,
		category : "Editing"
	};

};