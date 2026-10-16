time set noon
weather clear
execute as @a[tag=!miqa_done] at @s run function miqa:setup
execute as @a[tag=miqa_done] run tp @s 0.5 -60 3.5 0 22
scoreboard players add @a[tag=miqa_done] miqa 1
execute as @a[scores={miqa=1200}] run item replace entity @s weapon.mainhand with modern_industrialization:steam_mining_drill[modern_industrialization:activated=true,modern_industrialization:water=18000,modern_industrialization:steam_drill_fuel={burn_ticks:1000000,max_burn_ticks:1000000}]
