tag @s add miqa_done
gamemode creative @s
fill -8 -60 2 8 -54 16 air
setblock -6 -60 7 modern_industrialization:bronze_boiler
setblock -4 -60 7 modern_industrialization:bronze_furnace
setblock -2 -60 7 modern_industrialization:steel_macerator
setblock 0 -60 7 modern_industrialization:electric_furnace
setblock 2 -60 7 modern_industrialization:assembler
setblock -3 -60 5 modern_industrialization:bronze_barrel{item:{id:"minecraft:iron_ingot"},amt:1234L}
setblock -1 -60 5 modern_industrialization:steel_barrel{item:{id:"minecraft:diamond"},amt:64L}
setblock 1 -60 5 modern_industrialization:bronze_tank{fluid:{id:"minecraft:lava"},amt:16000L}
setblock 3 -60 5 modern_industrialization:steel_tank{fluid:{id:"minecraft:water"},amt:32000L}
summon minecraft:armor_stand 5 -60 5 {Rotation:[180f,0f],ShowArms:1b,equipment:{head:{id:"modern_industrialization:quantum_helmet",count:1},chest:{id:"modern_industrialization:quantum_chestplate",count:1},legs:{id:"modern_industrialization:quantum_leggings",count:1},feet:{id:"modern_industrialization:quantum_boots",count:1}}}
summon minecraft:armor_stand 6.5 -60 5 {Rotation:[180f,0f],ShowArms:1b,equipment:{head:{id:"modern_industrialization:rubber_helmet",count:1},chest:{id:"modern_industrialization:diesel_jetpack",count:1},feet:{id:"modern_industrialization:rubber_boots",count:1}}}
summon minecraft:armor_stand -5.5 -60 5 {Rotation:[180f,0f],ShowArms:1b,equipment:{chest:{id:"modern_industrialization:gravichestplate",count:1}}}
setblock 0 -60 12 modern_industrialization:electric_blast_furnace
item replace entity @s weapon.mainhand with modern_industrialization:wrench
tp @s 0.5 -60 3.5 facing 0.5 -59.5 7
fill -6 -60 10 -4 -58 12 modern_industrialization:steel_machine_casing
setblock -4 -59 11 minecraft:glass
setblock -6 -59 11 minecraft:glass
setblock -5 -58 11 minecraft:glass
setblock -5 -60 11 minecraft:glass
setblock -5 -59 12 minecraft:glass
setblock -5 -59 11 minecraft:air
setblock -5 -59 10 modern_industrialization:large_tank{facingDirection:2,stacks:[{id:"minecraft:water",amount:1200000}],capacity:1728000}
scoreboard objectives add miqa dummy
scoreboard players set @s miqa 0
