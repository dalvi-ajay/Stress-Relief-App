package com.example.data

enum class MoodState(
    val label: String,
    val suggestionTitle: String,
    val suggestionBody: String,
    val recommendedPattern: BreathingPattern,
    val recommendsBreathing: Boolean,
) {
    TENSE(
        label = "Tense",
        suggestionTitle = "Ease physical tension",
        suggestionBody = "Try 4-7-8 Calm breathing or a 40-second Shoulder & Jaw Release.",
        recommendedPattern = BreathingPattern.CALM_478,
        recommendsBreathing = false,
    ),
    OVERWHELMED(
        label = "Overwhelmed",
        suggestionTitle = "Slow down your nervous system",
        suggestionBody = "A quick Physiological Sigh (2x inhale, long exhale) rapidly lowers stress.",
        recommendedPattern = BreathingPattern.PHYSIOLOGICAL_SIGH,
        recommendsBreathing = true,
    ),
    RESTLESS(
        label = "Restless",
        suggestionTitle = "Steady your focus",
        suggestionBody = "Box Breathing (4-4-4-4) gives busy thoughts a calm, rhythmic anchor.",
        recommendedPattern = BreathingPattern.BOX,
        recommendsBreathing = true,
    ),
    OKAY(
        label = "Steady",
        suggestionTitle = "Maintain your calm",
        suggestionBody = "Coherent 5-5 breathing keeps your heart and breath in gentle balance.",
        recommendedPattern = BreathingPattern.COHERENT,
        recommendsBreathing = true,
    ),
}

enum class BreathingPattern(
    val title: String,
    val cadenceLabel: String,
    val tagline: String,
    val inhaleSec: Int,
    val holdSec: Int,
    val exhaleSec: Int,
    val restSec: Int,
) {
    BOX(
        title = "Box Breathing",
        cadenceLabel = "4 • 4 • 4 • 4",
        tagline = "Equal sides to steady focus and quiet racing thoughts",
        inhaleSec = 4,
        holdSec = 4,
        exhaleSec = 4,
        restSec = 4,
    ),
    CALM_478(
        title = "4-7-8 Calm",
        cadenceLabel = "4 • 7 • 8",
        tagline = "Extended exhale to soothe anxiety and prepare for rest",
        inhaleSec = 4,
        holdSec = 7,
        exhaleSec = 8,
        restSec = 0,
    ),
    COHERENT(
        title = "Coherent Pace",
        cadenceLabel = "5 • 5",
        tagline = "Smooth, continuous wave for balanced calm",
        inhaleSec = 5,
        holdSec = 0,
        exhaleSec = 5,
        restSec = 0,
    ),
    PHYSIOLOGICAL_SIGH(
        title = "Long Exhale Sigh",
        cadenceLabel = "3 • 1 • 6",
        tagline = "Fast stress reset with a slow, releasing exhale",
        inhaleSec = 3,
        holdSec = 1,
        exhaleSec = 6,
        restSec = 0,
    );

    val cycleSeconds: Int
        get() = inhaleSec + holdSec + exhaleSec + restSec
}

enum class BreathingPhase {
    READY,
    INHALE,
    HOLD,
    EXHALE,
    REST,
    COMPLETE,
}

data class ReliefTaskItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationLabel: String,
    val isCompleted: Boolean = false,
    val isCustom: Boolean = false,
)

data class GroundingSenseStep(
    val count: Int,
    val sense: String,
    val prompt: String,
    val exampleHint: String,
)

data class BodyReleaseStep(
    val id: String,
    val title: String,
    val instruction: String,
    val holdSeconds: Int,
)

object DefaultReliefContent {
    val presetTasks = listOf(
        ReliefTaskItem(
            id = "task_water",
            title = "Drink a slow glass of cool water",
            subtitle = "Notice the temperature and pause between sips",
            durationLabel = "1 min",
        ),
        ReliefTaskItem(
            id = "task_horizon",
            title = "Look 20 feet away from your screen",
            subtitle = "Soften your gaze toward a window or distant wall",
            durationLabel = "30 sec",
        ),
        ReliefTaskItem(
            id = "task_shoulders",
            title = "Unclench jaw & drop both shoulders",
            subtitle = "Let your tongue rest gently from the roof of your mouth",
            durationLabel = "30 sec",
        ),
        ReliefTaskItem(
            id = "task_stretch",
            title = "Roll wrists and stretch forearms",
            subtitle = "Slow circles both ways while breathing evenly",
            durationLabel = "1 min",
        ),
        ReliefTaskItem(
            id = "task_gratitude",
            title = "Name one small thing that went okay today",
            subtitle = "It can be as simple as warm light or a quiet minute",
            durationLabel = "1 min",
        ),
    )

    val groundingSteps = listOf(
        GroundingSenseStep(
            count = 5,
            sense = "See",
            prompt = "Look around slowly and notice 5 calm details in your space.",
            exampleHint = "Light on a wall, a plant leaf, the texture of fabric...",
        ),
        GroundingSenseStep(
            count = 4,
            sense = "Feel",
            prompt = "Notice 4 physical sensations supporting your body right now.",
            exampleHint = "Feet on the floor, cool air, the chair beneath you...",
        ),
        GroundingSenseStep(
            count = 3,
            sense = "Hear",
            prompt = "Listen gently for 3 background sounds near or far.",
            exampleHint = "Distant traffic, a soft fan, your own steady breath...",
        ),
        GroundingSenseStep(
            count = 2,
            sense = "Smell",
            prompt = "Take a soft breath in and notice 2 subtle scents.",
            exampleHint = "Fresh air, coffee or tea, clean laundry...",
        ),
        GroundingSenseStep(
            count = 1,
            sense = "Taste",
            prompt = "Notice 1 lingering taste or take a slow sip of water.",
            exampleHint = "Mint, tea, or simply a clean, unhurried breath...",
        ),
    )

    val bodyReleaseSteps = listOf(
        BodyReleaseStep(
            id = "jaw",
            title = "Soften Jaw & Brow",
            instruction = "Part your teeth slightly, let your tongue rest, and smooth the space between your eyebrows.",
            holdSeconds = 10,
        ),
        BodyReleaseStep(
            id = "shoulders",
            title = "Shoulder Drop",
            instruction = "Lift both shoulders gently toward your ears as you inhale, then let them fall completely on exhale.",
            holdSeconds = 10,
        ),
        BodyReleaseStep(
            id = "hands",
            title = "Open & Release Hands",
            instruction = "Curl your fingers softly into fists for a moment, then spread them wide and let them rest heavy.",
            holdSeconds = 10,
        ),
        BodyReleaseStep(
            id = "neck",
            title = "Gentle Neck Tilt",
            instruction = "Tilt your right ear lightly toward your right shoulder, pause, then float softly to the left.",
            holdSeconds = 10,
        ),
    )

    val worryBubbleLabels = listOf(
        "Racing thoughts",
        "Unread messages",
        "Tomorrow's list",
        "Tight shoulders",
        "Rushing",
        "Perfectionism",
        "Noise & clutter",
        "Self-doubt",
    )

    val kindPrompts = listOf(
        "You do not have to solve the entire week in the next ten minutes.",
        "Rest is not something you have to earn by running out of energy.",
        "One slow exhale is enough to remind your body that you are safe right now.",
        "Progress on a hard day can look like simply speaking gently to yourself.",
        "Let the next task be small, clear, and unhurried.",
        "Notice one thing around you that is quiet and steady.",
    )
}
