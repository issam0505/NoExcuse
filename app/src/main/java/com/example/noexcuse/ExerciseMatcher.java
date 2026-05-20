package com.example.noexcuse;

import java.util.Arrays;
import java.util.List;

/**
 * ExerciseMatcher
 *
 * Fuzzy-matches a user-supplied exercise name against the global exercise list.
 * Used by ExercisePreviewActivity to fix typos / casing before hitting the API.
 *
 * Strategy (in order):
 *  1. Exact match (case-insensitive)
 *  2. "Contains" match  – input contains a known name, or vice-versa
 *  3. Word-overlap score  – counts shared words (handles "Bench Pres" → "Bench Press")
 *  4. Levenshtein distance – catches pure typos
 *
 * Returns the best-matching canonical name, or the original input if nothing scores
 * above MIN_SIMILARITY threshold.
 */
public final class ExerciseMatcher {

    private ExerciseMatcher() {}

    // ── Minimum word-overlap ratio to accept a "word match"  (0–1) ─────────
    private static final double MIN_WORD_OVERLAP = 0.5;

    // ── Max Levenshtein distance (relative to candidate length) to accept ──
    private static final double MAX_LEV_RATIO    = 0.35;

    // ─────────────────────────────────────────────────────────────────────────
    //  Global exercise list
    // ─────────────────────────────────────────────────────────────────────────
    public static final List<String> ALL_EXERCISES = Arrays.asList(
            "3/4 Sit-Up", "90/90 Hamstring", "Ab Roller", "Adductor",
            "Adductor/Grood Flexibility Exercise", "Alternate Hammer Curl",
            "Alternate Heel Touchers", "Alternate Incline Dumbbell Curl",
            "Alternate Leg Muscle Pass", "Alternating Cable Shoulder Press",
            "Alternating Deltoid Raise", "Alternating Kettlebell Press",
            "Alternating Row", "Ankle Circles", "Ankle On Knee",
            "Anterior Mitchell Stretch", "Arm Circles", "Arnold Dumbbell Press",
            "Around The Worlds", "Atlas Stone Trainer", "Atlas Stones",
            "Axle Deadlift", "Back Flyes - With Bands", "Backward Drag",
            "Backward Medicine Ball Throw", "Bag Setup", "Band Hip Adductions",
            "Band Pull Apart", "Band Assisted Pull-Up",
            "Barbell Bench Press - Medium Grip", "Barbell Curl",
            "Barbell Curls Lying Against An Incline", "Barbell Deadlift",
            "Barbell Full Squat", "Barbell Glute Bridge",
            "Barbell Guillotine Bench Press",
            "Barbell Incline Bench Press - Medium Grip", "Barbell Lunge",
            "Barbell Rear Delt Row", "Barbell Rollout from Bench",
            "Barbell Shrug", "Barbell Shrug Behind The Back",
            "Barbell Side Bend", "Barbell Side Split Squat", "Barbell Squat",
            "Barbell Walking Lunge", "Battling Ropes", "Bear Crawl Sled Drags",
            "Behind The Neck Barbell Press", "Bench Jump",
            "Bench Press - Powerlifting", "Bench Press - With Bands",
            "Bench Press with Chains", "Bench Sprint",
            "Bent-Arm Barbell Pullover", "Bent-Arm Dumbbell Pullover",
            "Bent-Knee Hip Extension", "Bent Over Barbell Row",
            "Bent Over Dumbbell Rear Delt Raise With Head On Bench",
            "Bent Over Low-Pulley Side Lateral Raise",
            "Bent Over One-Arm Long Bar Row",
            "Bent Over Two-Arm Dumbbell Close-Grip Row",
            "Bent Over Two-Arm Dumbbell Row",
            "Bent Over Two-Arm Dumbbell Row With Incline Bench",
            "Biceps Curls With Bands", "Bicycling", "Bicycling, Stationary",
            "Board Press", "Body-Up", "Body Tricep Press", "Bodyweight Flyes",
            "Bodyweight Mid Row", "Bodyweight Squat",
            "Bosu Ball Cable Crunch with Side Bends", "Bottoms Up",
            "Bottoms-Up Clean From The Hang Position",
            "Box Jump (Multiple Response)", "Box Squat", "Box Squat with Bands",
            "Box Squat with Chains", "Brachialis-Smash", "Bridge", "Butt-Ups",
            "Butt Lift (Bridge)", "Butterfly", "Cable Chest Press",
            "Cable Crossover", "Cable Crunch", "Cable Deadlifts",
            "Cable Hip Adduction", "Cable Incline Pushdown",
            "Cable Internal Rotation", "Cable Iron Cross", "Cable Judo Flip",
            "Cable Lying Flye", "Cable One-Arm Lateral Raise",
            "Cable Preacher Curl", "Cable Rear Delt Fly",
            "Cable Reverse Crunch", "Cable Seated Crunch",
            "Cable Seated Lateral Raise", "Cable Shoulder Press", "Cable Shrug",
            "Cable Wrist Curl", "Calf Raise On A Dumbbell",
            "Calf Raises - With Bands", "Calf Stretch Elongated",
            "Calf Stretch Static Wall", "Calf-Machine Shoulder Shrug",
            "Car Deadlift", "Car Driver", "Carioca Quick Steps", "Cat Stretch",
            "Catch and Overhead Throw", "Chain Handle Extension", "Chain Press",
            "Chair Leg Extended Stretch", "Chest And Front Deltoid Stretch",
            "Chest Push (with ball)", "Chest Push from 3 Point Stance",
            "Chest Push with Run", "Chest Stretch On Stability Ball", "Chin-Up",
            "Chin To Chest Stretch", "Chins", "Circus Dumbbell", "Clean",
            "Clean And Jerk", "Clean and Press", "Clean Deadlift",
            "Clean From Blocks", "Clean Pull", "Clock Push-Up",
            "Close-Grip Barbell Bench Press", "Close-Grip Dumbbell Press",
            "Close-Grip Ez-Bar Curl", "Close-Grip Ez-Bar Press",
            "Close-Grip Front Lat Pulldown", "Close-Grip Push-Up",
            "Close-Grip Push-Up off a Dumbbell",
            "Close-Grip Standing Barbell Curl", "Cocoons", "Conan's Wheel",
            "Concentration Curls", "Crossover Reverse Lunge",
            "Crunch - Hands Overhead", "Crunch - Leg Labs", "Crunches",
            "Cuban Press", "Deadlift With Bands", "Deadlift With Chains",
            "Decline Barbell Bench Press", "Decline Close-Grip Bench Press",
            "Decline Dumbbell Bench Press", "Decline Dumbbell Flye",
            "Decline Dumbbell Triceps Extension",
            "Decline Ez-Bar Triceps Extension", "Decline Push-Up",
            "Decline Reverse Crunch", "Decline Smith Press", "Deficit Deadlift",
            "Delt Drive", "Diamond Push-Ups", "Diet Exercises", "Digger",
            "Dip Machine", "Dips - Chest Version", "Dips - Triceps Version",
            "Donkey Calf Raises", "Double Bowl Squat with Kettlebells",
            "Downward Facing Dog", "Drag Curl", "Drop Push",
            "Dumbbell Alternating Bicep Curl", "Dumbbell Bench Press",
            "Dumbbell Bicep Curl", "Dumbbell Clean", "Dumbbell Floor Press",
            "Dumbbell Flyes", "Dumbbell Incline Shoulder Raise",
            "Dumbbell Lunges", "Dumbbell Lying One-Arm Rear Delt Raise",
            "Dumbbell Lying Pronated Incline Row",
            "Dumbbell Lying Rear Delt Row", "Dumbbell One-Arm Shoulder Press",
            "Dumbbell One-Arm Triceps Extension",
            "Dumbbell One-Arm Upright Row", "Dumbbell Prone Incline Curl",
            "Dumbbell Raise", "Dumbbell Rear Delt Raise", "Dumbbell Scaption",
            "Dumbbell Seated Box Jump", "Dumbbell Seated One-Leg Calf Raise",
            "Dumbbell Shoulder Press", "Dumbbell Shrug", "Dumbbell Side Bend",
            "Dumbbell Side Lunge", "Dumbbell Squat", "Dumbbell Stepping",
            "Dumbbell Tricep Extension", "Dumbbell V-Sit Cross Jab",
            "Dynamic Back Stretch", "Dynamic Chest Stretch", "Elbow Circles",
            "Elbow To Knee", "Elbows Back Stretch", "Elevated Cable Rows",
            "Elevated Leg Crunches", "Elliptical Trainer",
            "Exercise Ball Crunch", "Exercise Ball Pull-In",
            "Extended Range One-Arm Kettlebell Floor Press",
            "External Rotation With Band", "External Rotation With Cable",
            "Ez-Bar Curl", "Ez-Bar Skullcrusher", "Face Pull", "Farmer's Walk",
            "Fast Skipping", "Finger Curls", "Flat Bench Cable Flyes",
            "Flat Bench Leg Pull-In", "Flat Bench Lying Leg Raise",
            "Flexibility Exercise", "Floor Glute-Ham Raise", "Floor Press",
            "Flutter Kicks", "Foot-Smash", "Forward Drag with Harness",
            "Forward Lunge", "Frankenstein Squat", "Freehand Jump Squat",
            "Frog Hops", "Frog Sit-Ups", "Front Barbell Squat",
            "Front Barbell Squat To A Bench", "Front Cable Raise",
            "Front Cone Jumps", "Front Dumbbell Raise",
            "Front Incline Dumbbell Raise", "Front Leg Raises",
            "Front Plate Raise", "Front Raise And Pullover",
            "Front Squat (Clean Grip)", "Front Squats With Bands",
            "Front Two-Dumbbell Raise",
            "Full Range 180 Degree Abdominal Crunch", "Furrow Stretch",
            "Gillingham Bench Press Routine", "Glute Ham Raise",
            "Glute Kickback", "Goblet Squat", "Good Morning",
            "Good Morning From Pins", "Gorilla Chin/Crunch",
            "Groin and Back Stretch", "Groiners", "Hack Squat",
            "Half Kneeling Ankle Dorsiflexion", "Hammer Curls",
            "Hammer Grip Incline DB Bench Press", "Hamstring-Smash",
            "Hamstring Stretch", "Handstand Push-Ups", "Hang Clean",
            "Hang Clean - Below Knees", "Hang High Pull", "Hang Snatch",
            "Hang Snatch - Below Knees", "Hanging Leg Raise", "Hanging Pike",
            "Hanging Straight Knee Raise", "Heavy Bag Thrust",
            "High Cable Crossover", "High Knee Jog In Place",
            "High Pull From Blocks", "Hip Circles", "Hip Extension with Bands",
            "Hip Flexion with Band", "Hip Lift with Band", "Hug A Tree",
            "Hug Me", "Hurdle Hops", "Hyperextensions (Back Extensions)",
            "Hyperextensions With Bands", "Iliotibial Band SMR", "Inchworm",
            "Incline Barbell Bench Press", "Incline Cable Chest Press",
            "Incline Cable Flye", "Incline Dumbbell Bench Press",
            "Incline Dumbbell Curl", "Incline Dumbbell Flye",
            "Incline Dumbbell Press", "Incline Inner Biceps Curl",
            "Incline Push-Up", "Incline Push-Up Close-Grip",
            "Incline Push-Up Depth Jump", "Incline Push-Up Reverse Grip",
            "Incline Push-Up Wide", "Intermediate Hip Flexor Stretch",
            "Intermediate Groin Stretch", "Internal Rotation with Band",
            "Inverted Row", "Inverted Row With Straps", "Iron Cross",
            "Iron Man", "Isolateral Touch Down", "It Band Stretch",
            "Jackknife Sit-Up", "Janda Sit-Up", "Jefferson Squats",
            "Jerk Balance", "Jerk Dip Squat", "JM Press", "Jog In Place",
            "Jogging, Treadmill", "Keg Toss", "Kettlebell One-Arm Clean",
            "Kettlebell One-Arm Clean And Jerk",
            "Kettlebell One-Arm Military Press To The Side",
            "Kettlebell One-Arm Row", "Kettlebell One-Arm Snatch",
            "Kettlebell Pirate Ships", "Kettlebell Pistol Squat",
            "Kettlebell Seesaw Press", "Kettlebell Side Windmill",
            "Kettlebell Sumo High Pull", "Kettlebell Thruster",
            "Kettlebell Turkish Get-Up", "Kettlebell Windmill",
            "Kicking Drill", "Knee Circles", "Knee Hip Extension",
            "Knee To Chest", "Kneeling Arm Drill", "Kneeling Cable Crunch",
            "Kneeling Cable Hip Extension", "Kneeling High Pulley Crunch",
            "Kneeling Hip Flexor Stretch", "Kneeling Jump Squat",
            "Kneeling Lat Pulldown", "Kneeling Low Pulley Deltoid Raise",
            "Kneeling One-Arm Row", "Kneeling One-Arm High Pulley Row",
            "Kneeling Same-Side Arm/Leg Raise",
            "Kneeling Single-Leg Hamstring Stretch", "Kneeling Squat",
            "Landmine Linear Jammer", "Landmine 180's", "Lat Pulldown",
            "Lateral Raise - With Bands", "Latissimus Dorsi SMR",
            "Leg-Up Crunch", "Leg Extensions", "Leg Press", "Leg Pull-In",
            "Leg Raises", "Linear Acceleration Wall Drills",
            "Linear Depth Jump", "Log Clean and Press", "Log Lift",
            "London Bridge", "Low Cable Crossover", "Low Cable Rooting",
            "Low Pulley Row To Neck", "Lower Back-Smash", "Lower Back SMR",
            "Lunge With Bicep Curl", "Lunge With Rotation",
            "Lying Cable Deltoid Raise", "Lying Cambered Barbell Row",
            "Lying Close-Grip Barbell Triceps Extension Behind The Head",
            "Lying Close-Grip Barbell Triceps Extension", "Lying Crunches",
            "Lying Dumbbell Chest Flye", "Lying Dumbbell Single-Leg Curl",
            "Lying Face Down Plate Neck Resistance",
            "Lying Face Up Plate Neck Resistance",
            "Lying High Bench Barbell Row", "Lying Leg Curls",
            "Lying Machine Squat", "Lying One-Arm Rear Delt Raise",
            "Lying Prone Incline Biceps Curl", "Lying Rear Delt Raise",
            "Lying Sciatic Nerve Stretch", "Lying Side Leg Raise",
            "Lying T-Bar Row", "Lying Two-Arm Dumbbell Triceps Extension",
            "Machine Bench Press", "Machine Bicep Curl", "Machine Chest Flye",
            "Machine Chest Press", "Machine Preacher Curl",
            "Machine Shoulder Press", "Machine Shrug",
            "Machine Triceps Extension", "Matrix Lunge",
            "Medicine Ball Chest Pass", "Medicine Ball Full Twist",
            "Medicine Ball Scoop Throw", "Mid-Back Shrug", "Middle Back SMR",
            "Military Press", "Modified Hurdler Stretch", "Monster Walks",
            "Moving Claw Series", "Muscle Up", "Narrow Stance Leg Press",
            "Narrow Stance Squats", "Natural Glute Ham Raise", "Neck-Smash",
            "Neck Bridge Prone", "One-Arm Arm Circle", "One-Arm Axle Deadlift",
            "One-Arm Dumbbell Bench Press", "One-Arm Dumbbell Incline Bench Press",
            "One-Arm Dumbbell Row", "One-Arm Flat Bench Dumbbell Flye",
            "One-Arm High-Pulley Cable Side Bends", "One-Arm Kettlebell Clean",
            "One-Arm Kettlebell Clean and Jerk", "One-Arm Kettlebell Floor Press",
            "One-Arm Kettlebell Military Press To The Side",
            "One-Arm Kettlebell Row", "One-Arm Kettlebell Snatch",
            "One-Arm Kettlebell Split Jerk", "One-Arm Kettlebell Split Snatch",
            "One-Arm Long Bar Row", "One-Arm Medicine Ball Slam",
            "One-Arm Open Palm Dumbbell Clean",
            "One-Arm Overhead Kettlebell Squat", "One-Arm Side Deadlift",
            "One-Arm Side Laterals", "One-Legged Cable Kickback",
            "One-Legged Extension", "One Legged Squat",
            "Open Palm Dumbbell Clean", "Overhead Cable Curl",
            "Overhead Dumbbell Front Raise", "Overhead Dumbbell Triceps Extension",
            "Overhead Lat Stretch", "Overhead Long Bar Tricpes Extension",
            "Overhead Medicine Ball Slam", "Overhead Slam", "Overhead Squat",
            "Oxygen Collector", "Pallof Press With Rotation",
            "Palms-Down Barbell Wrist Curl Over A Bench",
            "Palms-Down Dumbbell Wrist Curl Over A Bench",
            "Palms-Up Barbell Wrist Curl Over A Bench",
            "Palms-Up Dumbbell Wrist Curl Over A Bench",
            "Parallel Bar Dip", "Pelvic Tilt Into Bridge", "Pelvic Tilt",
            "Peroneal SMR", "Phosphate Pause Bench Press", "Pike-To-Muscle-Up",
            "Pin Presses", "Piriformis SMR", "Plank", "Plank With Leg Lift",
            "Plate Twist", "Platform Bench Press", "Plie Dumbbell Squat",
            "Plio Push-Up", "Pogo Hop", "Power Clean", "Power Clean From Blocks",
            "Power Jerk", "Power Partial Lateral Raise", "Power Snatch",
            "Power Snatch From Blocks", "Prone Manual Neck Resistance",
            "Pull-Up", "Pull-Through", "Push-Up", "Push-Up Plus",
            "Push-Ups With Feet On An Exercise Ball",
            "Push-Ups With Feet Elevated", "Push-Ups With Pack", "Pushdown",
            "Pushdown - Close-Grip", "Pushdown - Wide-Grip", "Pyramid",
            "Quadriceps SMR", "Quadriceps Stretch", "Quarter Squat",
            "Rack Delivery", "Rack Pull", "Rear Delt Flye", "Rear Delt Raise",
            "Recumbent Bike", "Return To Cratch Position",
            "Reverse Barbell Curl", "Reverse Barbell Wrist Curl Over A Bench",
            "Reverse Cable Crunch", "Reverse Crunch", "Reverse Decline Crunch",
            "Reverse Dumbbell Bicep Curl", "Reverse Flyes",
            "Reverse Grip Bent-Over Barbell Row",
            "Reverse Grip Cable Incline Pushdown",
            "Reverse Grip Incline Bench Press",
            "Reverse Grip Triceps Pushdown", "Reverse Hyper", "Reverse Lunge",
            "Reverse Machine Flyes", "Reverse Pec Deck Flye",
            "Reverse Plate Curls", "Reverse Triceps Pushdown", "Rhomboid SMR",
            "Rider", "Rocking Frog Stretch", "Rocky Pull-Ups/Chins",
            "Romanian Deadlift", "Romanian Deadlift From Deficit",
            "Romanian Deadlift With Dumbbells", "Rope Climb", "Rope Crunch",
            "Rowing, Stationary", "Runner's Lunge", "Running, Treadmill",
            "Russian Twist", "Sandbag Load", "Scapular Pull-Up",
            "Scissor Kick", "Scissors", "Seated Barbell Twist",
            "Seated Bent-Over Dumbbell Rear Delt Raise", "Seated Biceps Curl",
            "Seated Cable Rows", "Seated Calf Raise",
            "Seated Close-Grip Concentrated Curl", "Seated Dumbbell Curl",
            "Seated Dumbbell Inner Biceps Curl",
            "Seated Dumbbell Palms-Down Wrist Curl",
            "Seated Dumbbell Palms-Up Wrist Curl", "Seated Dumbbell Press",
            "Seated Dumbbell Rear Delt Raise", "Seated Dumbbell Shoulder Press",
            "Seated Flat Bench Leg Pull-In", "Seated Good Morning",
            "Seated Head Harness Neck Resistance", "Seated Leg Curl",
            "Seated Leg Press",
            "Seated One-Arm Dumbbell Palms-Down Wrist Curl",
            "Seated One-Arm Dumbbell Palms-Up Wrist Curl",
            "Seated One-Arm Dumbbell Overhead Triceps Extension",
            "Seated Overhead Dumbbell Triceps Extension",
            "Seated Palms-Down Barbell Wrist Curl",
            "Seated Palms-Up Barbell Wrist Curl", "Seated Reverse Flye",
            "Seated Side Lateral Raise", "Seated Triceps Press",
            "Seated Two-Arm Overhead Dumbbell Triceps Extension", "Second Pull",
            "See-Saw Press", "Segment Snatch", "Single Straight Leg Raise",
            "Single Dumbbell Shoulder Press", "Single Leg Glute Bridge",
            "Single Leg Push-Off", "Single Leg Lying Calf Raise",
            "Single Leg Calf Raise", "Shotgun Row", "Shoulder Circles",
            "Shoulder Press - With Bands", "Shoulder Raise", "Shoulder Stretch",
            "Shrug With Bands", "Shrug With Chains", "Side-To-Side Chins",
            "Side Bridge", "Side Hop-Up", "Side Lateral Raise",
            "Side Lateral Raise With Bands", "Side Lunge",
            "Side Lunge With Dumbbells", "Side Neck Stretch", "Side Wrist Pull",
            "Sit-Up", "Skating", "Sledgehammer Swings", "Sled Push",
            "Smith Machine Behind The Neck Press", "Smith Machine Bench Press",
            "Smith Machine Calf Raise", "Smith Machine Close-Grip Bench Press",
            "Smith Machine Decline Press", "Smith Machine Incline Bench Press",
            "Smith Machine Incline Shoulder Press", "Smith Machine Low Row",
            "Smith Machine Overhead Press", "Smith Machine Pistol Squat",
            "Smith Machine Shrug", "Smith Machine Squat",
            "Smith Machine Upright Row", "Snatch", "Snatch Balance",
            "Snatch Deadlift", "Snatch From Blocks", "Snatch Pull",
            "Soleus SMR", "Speed Box Squat", "Speed Box Squat With Bands",
            "Speed Box Squat With Chains", "Speed Squats", "Spellcaster",
            "Spider Crawl", "Spider Curl", "Spinal Stretch", "Split Clean",
            "Split Jerk", "Split Jump", "Split Snatch", "Split Squat",
            "Split Squats With Dumbbells", "Squat To Bench", "Squat With Bands",
            "Squat With Chains", "Squats - With Bands", "Stairmaster",
            "Standing Barbell Press", "Standing Barbell Twist",
            "Standing Biceps Curl", "Standing Biceps Curls With Bands",
            "Standing Bradford Press", "Standing Cable Chest Press",
            "Standing Cable Crunch", "Standing Cable Low-to-High Chest Flye",
            "Standing Cable High-to-Low Chest Flye", "Standing Cable Lift",
            "Standing Cable Row", "Standing Cable Woodchop",
            "Standing Calf Raises", "Standing Close-Grip Concentration Curl",
            "Standing Concentrated Dumbbell Curl",
            "Standing Dumbbell Triceps Extension",
            "Standing Dumbbell Calf Raise", "Standing Dumbbell Cherry Pickers",
            "Standing Dumbbell Compression", "Standing Dumbbell Incline Curl",
            "Standing Dumbbell Press", "Standing Dumbbell Reverse Curl",
            "Standing Dumbbell Shrug", "Standing Dumbbell Triceps Extension",
            "Standing Dumbbell Upright Row", "Standing Elevated Leg Curl",
            "Standing Ez-Bar French Press",
            "Standing Front Barbell Raise Over Head",
            "Standing Inner-Biceps Curl", "Standing Lateral Raise",
            "Standing Leg Rollout", "Standing Long Bar Back Row",
            "Standing Low-Pulley Deltoid Raise", "Standing Military Press",
            "Standing One-Arm Cable Curl",
            "Standing One-Arm Dumbbell Curl Over Preacher Bench",
            "Standing One-Arm Dumbbell Triceps Extension",
            "Standing One-Leg Cable Curl", "Standing One-Leg Calf Raise",
            "Standing Overhead Barbell Triceps Extension",
            "Standing Palms-Down Barbell Wrist Curl",
            "Standing Palms-Up Barbell Wrist Curl", "Standing Pelvic Tilt",
            "Standing Rear Delt Row", "Standing Reverse Barbell Curl",
            "Standing Soles Stretch", "Standing Toe Touches",
            "Standing Two-Arm Overhead Dumbbell Triceps Extension",
            "Step-up With Knee Raise", "Step-ups",
            "Stiff-Legged Barbell Deadlift", "Stiff-Legged Dumbbell Deadlift",
            "Stomach Vacuum", "Straight-Arm Barbell Pullover",
            "Straight-Arm Dumbbell Pullover", "Straight-Arm Pulldown",
            "Straight Leg Raise To The Side", "Stretch To The Side",
            "Suitcase Deadlift", "Sumo Deadlift", "Sumo Deadlift With Bands",
            "Sumo Deadlift With Chains", "Supermans",
            "Sustained Vascular Pull-Up", "T-Bar Row", "T-Bar Row With Handle",
            "Table Top Plank", "Tate Press", "The Clean", "Thoracic SMR",
            "Thigh Adductor Stretch", "Three-Quarter Sit-Up", "Thrusters",
            "Tibialis Anterior SMR", "Toe Touchers", "Torso Twist",
            "Trailing Leg Stretch", "Trap Bar Deadlift",
            "Tricep Dumbbell Kickback", "Triceps Pushdown",
            "Triceps Pushdown - Rope Attachment",
            "Triceps Pushdown - V-Bar Attachment", "Triceps Stretch",
            "Trunk Rotator", "Turbine", "Turkish Get-Up",
            "Two-Arm Kettlebell Clean", "Two-Arm Kettlebell Jerk",
            "Two-Arm Kettlebell Military Press", "Two-Arm Kettlebell Row",
            "Two-Arm Kettlebell Snatch", "Two-Arm Kettlebell Split Jerk",
            "Underhand Cable Pulldown", "Upper Back Stretch",
            "Upright Barbell Row", "Upright Cable Row",
            "Upright Row - With Bands", "V-Bar Pulldown", "V-Sit Cross Jab",
            "Vertical Leg Crunch", "Walking Lunge", "Weighted Ball Side Bend",
            "Weighted Bench Dip", "Weighted Crunches", "Weighted Pull-Up",
            "Wide-Grip Barbell Bench Press",
            "Wide-Grip Decline Barbell Bench Press",
            "Wide-Grip Incline Barbell Bench Press", "Wide-Grip Lat Pulldown",
            "Wide-Grip Rear Pull-Up", "Wide-Grip Upright Row",
            "Wide Stance Barbell Squat", "Wide Stance Leg Press",
            "Wide Stance Stiff-Legged Deadlift", "Windprints", "Wipers",
            "Wrist Circles", "Wrist Roller", "Wrist Rotations with Dumbbells",
            "Y-Raise", "Zottman Curl", "Zottman Preacher Curl"
    );

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the best-matching canonical exercise name for the given input.
     * If no good match is found, returns the original input unchanged.
     *
     * @param input  Raw name typed / stored by the user (may have typos / wrong case)
     * @return       Canonical exercise name from ALL_EXERCISES, or input if no match
     */
    public static String findBestMatch(String input) {
        if (input == null || input.trim().isEmpty()) return input;

        String query = input.trim();
        String queryLower = query.toLowerCase();

        // ── 1. Exact match (case-insensitive) ─────────────────────────────
        for (String candidate : ALL_EXERCISES) {
            if (candidate.equalsIgnoreCase(query)) return candidate;
        }

        // ── 2. Contains match ──────────────────────────────────────────────
        // a) known name is fully contained in query
        for (String candidate : ALL_EXERCISES) {
            if (queryLower.contains(candidate.toLowerCase())) return candidate;
        }
        // b) query is fully contained in known name
        for (String candidate : ALL_EXERCISES) {
            if (candidate.toLowerCase().contains(queryLower)) return candidate;
        }

        // ── 3. Word-overlap scoring ────────────────────────────────────────
        String[] queryWords = queryLower.split("[\\s\\-/(),.]+");
        String bestWordMatch = null;
        double bestWordScore = MIN_WORD_OVERLAP - 0.001;

        for (String candidate : ALL_EXERCISES) {
            String[] cWords = candidate.toLowerCase().split("[\\s\\-/(),.]+");
            double overlap = wordOverlap(queryWords, cWords);
            if (overlap > bestWordScore) {
                bestWordScore = overlap;
                bestWordMatch = candidate;
            }
        }
        if (bestWordMatch != null) return bestWordMatch;

        // ── 4. Levenshtein distance ────────────────────────────────────────
        String bestLevMatch = null;
        double bestLevRatio = Double.MAX_VALUE;

        for (String candidate : ALL_EXERCISES) {
            int dist = levenshtein(queryLower, candidate.toLowerCase());
            double ratio = (double) dist / Math.max(queryLower.length(), candidate.length());
            if (ratio < MAX_LEV_RATIO && ratio < bestLevRatio) {
                bestLevRatio = ratio;
                bestLevMatch = candidate;
            }
        }
        if (bestLevMatch != null) return bestLevMatch;

        // ── No match found → return as-is ────────────────────────────────
        return input;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Jaccard-style word overlap: |intersection| / |union|
     */
    private static double wordOverlap(String[] a, String[] b) {
        int matches = 0;
        for (String wa : a) {
            for (String wb : b) {
                if (wa.equals(wb)) { matches++; break; }
            }
        }
        int union = a.length + b.length - matches;
        return union == 0 ? 0 : (double) matches / union;
    }

    /**
     * Standard iterative Levenshtein distance.
     */
    private static int levenshtein(String a, String b) {
        int la = a.length(), lb = b.length();
        int[] prev = new int[lb + 1];
        int[] curr = new int[lb + 1];
        for (int j = 0; j <= lb; j++) prev[j] = j;
        for (int i = 1; i <= la; i++) {
            curr[0] = i;
            for (int j = 1; j <= lb; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(
                        Math.min(curr[j - 1] + 1, prev[j] + 1),
                        prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[lb];
    }
}