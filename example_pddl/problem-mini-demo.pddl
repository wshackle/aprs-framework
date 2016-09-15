(define (problem kitting-problem)
	(:domain kitting-domain)
	(:objects
		robot_1 - Robot
		changing_station_1 - EndEffectorChangingStation
		kit_tray_1 - KitTray
		kit_s2b2 - Kit
		empty_kit_tray_supply - LargeBoxWithEmptyKitTrays
		finished_kit_receiver - LargeBoxWithKits
		work_table_1 - WorkTable
		part_small_gear_tray part_medium_gear_tray part_large_gear_tray - PartsTray
		small_gear_1 small_gear_2  small_gear_3 small_gear_4 - Part
		medium_gear_1 medium_gear_2  medium_gear_3 medium_gear_4 - Part - Part
		large_gear_1 large_gear_2 - Part 
		part_gripper tray_gripper - EndEffector
		part_gripper_holder tray_gripper_holder - EndEffectorHolder
		sku_part_small_gear sku_part_medium_gear sku_part_large_gear - StockKeepingUnit
		sku_kit_s2b2_vessel - StockKeepingUnit
	)
	(:init
		(endEffector-has-physicalLocation-refObject-robot part_gripper robot_1)
		(endEffectorHolder-has-physicalLocation-refObject-changingStation part_gripper_holder changing_station_1)
		(endEffectorHolder-has-physicalLocation-refObject-changingStation tray_gripper_holder changing_station_1)
		(endEffector-has-physicalLocation-refObject-endEffectorHolder tray_gripper tray_gripper_holder)
		(endEffector-has-no-heldObject part_gripper)
		(endEffector-is-for-partSKU part_gripper sku_part_small_gear)
		(endEffector-is-for-partSKU part_gripper sku_part_medium_gear)
		(endEffector-is-for-partSKU part_gripper sku_part_large_gear)
		(endEffectorHolder-has-endEffector tray_gripper_holder part_gripper)
		(endEffectorChangingStation-has-endEffectorHolder changing_station_1 part_gripper_holder)
		(endEffectorChangingStation-has-endEffectorHolder changing_station_1 tray_gripper_holder)
		(robot-has-endEffector robot_1 part_gripper)
		(kit-has-kitTray kit_s2b2 kit_tray_1)
		(partsVessel-has-part  part_small_gear_tray small_gear_1)
		(partsVessel-has-part  part_small_gear_tray small_gear_2)
		(partsVessel-has-part  part_small_gear_tray small_gear_3)
		(partsVessel-has-part  part_small_gear_tray small_gear_4)
		(partsVessel-has-part  part_medium_gear_tray medium_gear_1)
		(partsVessel-has-part  part_medium_gear_tray medium_gear_2)
		(partsVessel-has-part  part_medium_gear_tray medium_gear_3)
		(partsVessel-has-part  part_medium_gear_tray medium_gear_4)
		(partsVessel-has-part  part_large_gear_tray large_gear_1)
		(partsVessel-has-part  part_large_gear_tray large_gear_2)
		;(workTable-has-no-objectOnTable work_table_1)
		(workTable-has-objectOnTable-kit work_table_1 kit_s2b2)
		;(kitTray-has-physicalLocation-refObject-lbwekt kit_tray_1 empty_kit_tray_supply)
(kitTray-has-physicalLocation-refObject-workTable kit_tray_1 work_table_1)
		(kitTray-has-physicalLocation-refObject-kit kit_tray_1 kit_s2b2)
		(kitTray-has-skuObject-sku kit_tray_1 sku_kit_s2b2_vessel)
		(part-has-physicalLocation-refObject-partsTray small_gear_1 part_small_gear_tray)
		(part-has-physicalLocation-refObject-partsTray small_gear_2 part_small_gear_tray)
		(part-has-physicalLocation-refObject-partsTray small_gear_3 part_small_gear_tray)
		(part-has-physicalLocation-refObject-partsTray small_gear_4 part_small_gear_tray)
		(part-has-physicalLocation-refObject-partsTray medium_gear_1 part_medium_gear_tray)
		(part-has-physicalLocation-refObject-partsTray medium_gear_2 part_medium_gear_tray)
		(part-has-physicalLocation-refObject-partsTray medium_gear_3 part_medium_gear_tray)
		(part-has-physicalLocation-refObject-partsTray medium_gear_4 part_medium_gear_tray)
		(part-has-physicalLocation-refObject-partsTray large_gear_1 part_large_gear_tray)
		(part-has-physicalLocation-refObject-partsTray large_gear_2 part_large_gear_tray)
		(part-has-skuObject-sku small_gear_1 sku_part_small_gear)
		(part-has-skuObject-sku small_gear_2 sku_part_small_gear)
		(part-has-skuObject-sku small_gear_3 sku_part_small_gear)
		(part-has-skuObject-sku small_gear_4 sku_part_small_gear)
		(part-has-skuObject-sku medium_gear_1 sku_part_medium_gear)
		(part-has-skuObject-sku medium_gear_2 sku_part_medium_gear)
		(part-has-skuObject-sku medium_gear_3 sku_part_medium_gear)
		(part-has-skuObject-sku medium_gear_4 sku_part_medium_gear)
		(part-has-skuObject-sku large_gear_1 sku_part_large_gear)
		(part-has-skuObject-sku large_gear_2 sku_part_large_gear)
		(endEffector-is-for-kitTraySKU tray_gripper sku_kit_s2b2_vessel)
		(= (capacity-of-parts-in-kit  sku_part_small_gear kit_s2b2) 0)
		(= (capacity-of-parts-in-kit  sku_part_medium_gear kit_s2b2) 0)
		(= (capacity-of-parts-in-kit  sku_part_large_gear kit_s2b2) 2)
		(= (quantity-of-parts-in-kit  sku_part_small_gear kit_s2b2) 0)
		(= (quantity-of-parts-in-kit  sku_part_medium_gear kit_s2b2) 0)
		(= (quantity-of-parts-in-kit  sku_part_large_gear kit_s2b2) 0)
		(= (quantity-of-parts-in-partstray  part_small_gear_tray) 4)
		(= (quantity-of-parts-in-partstray  part_medium_gear_tray) 4)
		(= (quantity-of-parts-in-partstray  part_large_gear_tray) 2)
		(= (part-found-flag) 1)
		(= (capacity-of-kits-in-lbwk finished_kit_receiver) 12)
		(= (quantity-of-kittrays-in-lbwekt empty_kit_tray_supply) 1)
		(= (quantity-of-kits-in-lbwk finished_kit_receiver) 0)
		(= (current-quantity-of-parts-in-kit kit_s2b2) 0)

(kit-exists kit_s2b2)
		;(= (final-quantity-of-parts-in-kit kit_s2b2) 4)
	)
	(:goal
		(and
		;(kit-has-physicalLocation-refObject-lbwk kit_s2b2 finished_kit_receiver)
		;(lbwk-has-kit finished_kit_receiver kit_s2b2)
		;(kit-has-physicalLocation-refObject-workTable kit_s2b2 work_table_1)
		;(workTable-has-objectOnTable-kit work_table_1 kit_s2b2)
(= (final-quantity-of-parts-in-kit kit_s2b2) 4)
		(= (quantity-of-parts-in-kit  sku_part_small_gear kit_s2b2) (capacity-of-parts-in-kit  sku_part_small_gear kit_s2b2))
		(= (quantity-of-parts-in-kit  sku_part_medium_gear kit_s2b2) (capacity-of-parts-in-kit  sku_part_medium_gear kit_s2b2))
		(= (quantity-of-parts-in-kit  sku_part_large_gear kit_s2b2) (capacity-of-parts-in-kit  sku_part_large_gear kit_s2b2))
		)
	)
)
