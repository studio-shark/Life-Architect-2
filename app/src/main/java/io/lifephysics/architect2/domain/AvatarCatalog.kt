package io.lifephysics.architect2.domain

import io.lifephysics.architect2.R

object AvatarCatalog {
    val all: List<Avatar> = listOf(
        // Tier 1
        Avatar(id = 1, name = "Wandering Fox", price = 0, drawableRes = R.drawable.fox_tier_1),
        Avatar(id = 2, name = "Goggled Rabbit", price = 100, drawableRes = R.drawable.rabbit_tier_1),
        Avatar(id = 3, name = "Masked Cat", price = 200, drawableRes = R.drawable.cat_tier_1),
        Avatar(id = 4, name = "Compass Badger", price = 250, drawableRes = R.drawable.badger_tier_1),
        Avatar(id = 5, name = "Hatted Squirrel", price = 300, drawableRes = R.drawable.squirrel_tier_1),
        Avatar(id = 6, name = "Scarfed Otter", price = 350, drawableRes = R.drawable.otter_tier_1),
        Avatar(id = 7, name = "Patched Raccoon", price = 400, drawableRes = R.drawable.raccoon_tier_1),
        Avatar(id = 8, name = "Capped Hedgehog", price = 450, drawableRes = R.drawable.hedgehog_tier_1),
        Avatar(id = 9, name = "Spectacled Hamster", price = 500, drawableRes = R.drawable.hamster_tier_1),
        Avatar(id = 10, name = "Hooded Ferret", price = 550, drawableRes = R.drawable.ferret_tier_1),
        // Tier 2
        Avatar(id = 11, name = "Ranger Wolf", price = 600, drawableRes = R.drawable.wolf_tier_2),
        Avatar(id = 12, name = "Monocled Deer", price = 700, drawableRes = R.drawable.deer_tier_2),
        Avatar(id = 13, name = "Helmeted Boar", price = 800, drawableRes = R.drawable.boar_tier_2),
        Avatar(id = 14, name = "Goggled Lynx", price = 900, drawableRes = R.drawable.lynx_tier_2),
        Avatar(id = 15, name = "Crowned Peacock", price = 1000, drawableRes = R.drawable.peacock_tier_2),
        Avatar(id = 16, name = "Masked Panther", price = 1100, drawableRes = R.drawable.panther_tier_2),
        Avatar(id = 17, name = "Scarred Hyena", price = 1200, drawableRes = R.drawable.hyena_tier_2),
        Avatar(id = 18, name = "Cloaked Raven", price = 1300, drawableRes = R.drawable.raven_tier_2),
        Avatar(id = 19, name = "Bandaged Crocodile", price = 1400, drawableRes = R.drawable.crocodile_tier_2),
        Avatar(id = 20, name = "Armored Tortoise", price = 1500, drawableRes = R.drawable.tortoise_tier_2),
        // Tier 3
        Avatar(id = 21, name = "Ironclad Bear", price = 2000, drawableRes = R.drawable.bear_tier_3),
        Avatar(id = 22, name = "Ashborn Jackal", price = 2500, drawableRes = R.drawable.jackal_tier_3),
        Avatar(id = 23, name = "Frost Owl", price = 3000, drawableRes = R.drawable.owl_tier_3),
        Avatar(id = 24, name = "Phantom Cobra", price = 3500, drawableRes = R.drawable.cobra_tier_3),
        Avatar(id = 25, name = "Storm Hawk", price = 4000, drawableRes = R.drawable.hawk_tier_3),
        Avatar(id = 26, name = "Dusk Bat", price = 4500, drawableRes = R.drawable.bat_tier_3),
        Avatar(id = 27, name = "Crimson Mantis", price = 5000, drawableRes = R.drawable.mantis_tier_3),
        Avatar(id = 28, name = "Sage Elephant", price = 5500, drawableRes = R.drawable.elephant_tier_3),
        Avatar(id = 29, name = "Void Chameleon", price = 6000, drawableRes = R.drawable.chameleon_tier_3),
        Avatar(id = 30, name = "Runic Gorilla", price = 6500, drawableRes = R.drawable.gorilla_tier_3),
        // Tier 4
        Avatar(id = 31, name = "Abyssal Shark", price = 7000, drawableRes = R.drawable.shark_tier_4),
        Avatar(id = 32, name = "Celestial Crane", price = 8000, drawableRes = R.drawable.crane_tier_4),
        Avatar(id = 33, name = "Obsidian Scorpion", price = 9000, drawableRes = R.drawable.scorpion_tier_4),
        Avatar(id = 34, name = "Starfall Panda", price = 10000, drawableRes = R.drawable.panda_tier_4),
        Avatar(id = 35, name = "Inferno Tiger", price = 11000, drawableRes = R.drawable.tiger_tier_4),
        Avatar(id = 36, name = "Glacial Mammoth", price = 12000, drawableRes = R.drawable.mammoth_tier_4),
        Avatar(id = 37, name = "Emperor Peacock", price = 13000, drawableRes = R.drawable.peacock_2_tier_4),
        Avatar(id = 38, name = "Sovereign Lion", price = 14000, drawableRes = R.drawable.lion_tier_4),
        Avatar(id = 39, name = "Colossus Rhino", price = 15000, drawableRes = R.drawable.rhino_tier_4),
        Avatar(id = 40, name = "Warlord Komodo", price = 16000, drawableRes = R.drawable.komododragon_tier_4),
        // Tier 5
        Avatar(id = 41, name = "Dawnbreaker Griffin", price = 20000, drawableRes = R.drawable.griffin_tier_5),
        Avatar(id = 42, name = "Eclipse Reaper Wolf", price = 25000, drawableRes = R.drawable.wolf_2_tier_5),
        Avatar(id = 43, name = "Eternal Flame Phoenix", price = 30000, drawableRes = R.drawable.phoenix_tier_5),
        Avatar(id = 44, name = "Void Ascendant Serpent", price = 35000, drawableRes = R.drawable.serpent_tier_5),
        Avatar(id = 45, name = "Starborn Kirin", price = 40000, drawableRes = R.drawable.kirin_tier_5),
        Avatar(id = 46, name = "Abyssal God Leviathan", price = 50000, drawableRes = R.drawable.leviathan_tier_5),
        Avatar(id = 47, name = "Celestial King Qilin", price = 60000, drawableRes = R.drawable.qilin_tier_5),
        Avatar(id = 48, name = "Runic Deity Fenrir", price = 75000, drawableRes = R.drawable.fenrir_tier_5),
        Avatar(id = 49, name = "Phantom God Anubis", price = 90000, drawableRes = R.drawable.anubis_tier_5),
        Avatar(id = 50, name = "The Architect", price = 100000, drawableRes = R.drawable.dragon_tier_5)
    )
}
