(define (domain kitting-domain)
	(:requirements :action-costs :derived-predicates :equality :fluents :strips :typing)
	(:types 
		EndEffector
		EndEffectorChangingStation
		EndEffectorHolder
		Kit
		KitTray
		LargeBoxWithEmptyKitTrays
		LargeBoxWithKits
		Part
		PartsTray
		Robot
		StockKeepingUnit
		WorkTable)
	(:predicates
		;?part is held by ?endeffector
		(part-has-physicalLocation-refObject-endEffector ?part - Part ?endeffector - EndEffector)

		;?partsTray contains ?part
		(partsVessel-has-part ?partstray - PartsTray ?part - Part)

		;?part has ?sku
		(part-has-skuObject-sku ?part - Part ?stockkeepingunit - StockKeepingUnit)

		;?robot has no end effector
		(robot-has-no-endEffector ?robot - Robot)

		;?endeffector is holding ?kit
		(endEffector-has-heldObject-kit ?endeffector - EndEffector ?kit - Kit)

		;?worktable has nothing on top of it
		(workTable-has-no-objectOnTable ?worktable - WorkTable)

		;?kittray is held by ?endeffector
		(kitTray-has-physicalLocation-refObject-endEffector ?kittray - KitTray ?endeffector - EndEffector)

		;?endeffectorholder is holding ?endeffector
		(endEffectorHolder-has-endEffector ?endeffectorholder - EndEffectorHolder ?endeffector - EndEffector)

		;?endEffector is for handling kit trays that have ?sku
		(endEffector-is-for-kitTraySKU ?endeffector - EndEffector ?stockkeepingunit - StockKeepingUnit)

		;?part is in ?partsTray
		(part-has-physicalLocation-refObject-partsTray ?part - Part ?partstray - PartsTray)

		;?kittray is on ?worktable
		(kitTray-has-physicalLocation-refObject-workTable ?kittray - KitTray ?worktable - WorkTable)

		;?endEffector is for handling parts that have ?sku
		(endEffector-is-for-partSKU ?endeffector - EndEffector ?stockkeepingunit - StockKeepingUnit)

		;?endeffector is located in ?endEffectorHolder
		(endEffector-has-physicalLocation-refObject-endEffectorHolder ?endeffector - EndEffector ?endeffectorholder - EndEffectorHolder)

		;?kit has ?kittray
		(kit-has-kitTray ?kit - Kit ?kittray - KitTray)

		;?lbwk contains ?kit
		(lbwk-has-kit ?largeboxwithkits - LargeBoxWithKits ?kit - Kit)

		;?worktable has ?kit on top of it
		(workTable-has-objectOnTable-kit ?worktable - WorkTable ?kit - Kit)

		;?worktable has ?kitTray on top of it
		(workTable-has-objectOnTable-kitTray ?worktable - WorkTable ?kittray - KitTray)

		;?endeffector is attached to ?robot
		(endEffector-has-physicalLocation-refObject-robot ?endeffector - EndEffector ?robot - Robot)

		;?part is found
		(part-is-found ?part - Part)

		;?slot is found
		(slot-is-found ?sku - StockKeepingUnit)

		;preset grasp for sku of part for approach and pick-up
		(grasp-is-set ?sku - StockKeepingUnit)

		;?kittray is located in ?lbwekt
		(kitTray-has-physicalLocation-refObject-lbwekt ?kittray - KitTray ?largeboxwithemptykittrays - LargeBoxWithEmptyKitTrays)

		;?kit is on ?worktable
		(kit-has-physicalLocation-refObject-workTable ?kit - Kit ?worktable - WorkTable)

		;?kittray has ?sku
		(kitTray-has-skuObject-sku ?kittray - KitTray ?stockkeepingunit - StockKeepingUnit)

		;?changingstation contains ?endeffectorholder
		(endEffectorChangingStation-has-endEffectorHolder ?endeffectorchangingstation - EndEffectorChangingStation ?endeffectorholder - EndEffectorHolder)

		;?kittray is related to ?kit
		(kitTray-has-physicalLocation-refObject-kit ?kittray - KitTray ?kit - Kit)

		;?kit is located in ?lbwk
		(kit-has-physicalLocation-refObject-lbwk ?kit - Kit ?largeboxwithkits - LargeBoxWithKits)

		;?endeffector is not holding any object
		(endEffector-has-no-heldObject ?endeffector - EndEffector)

		;?kit exists
		(kit-exists ?kit - Kit)

		;?part is in ?kit
		(part-has-physicalLocation-refObject-kit ?part - Part ?kit - Kit)

		;?endeffector is holding ?kittray
		(endEffector-has-heldObject-kitTray ?endeffector - EndEffector ?kittray - KitTray)

		;?endeffector is holding ?part
		(endEffector-has-heldObject-part ?endeffector - EndEffector ?part - Part)

		;?endeffectorholder is located in ?changingstation
		(endEffectorHolder-has-physicalLocation-refObject-changingStation ?endeffectorholder - EndEffectorHolder ?endeffectorchangingstation - EndEffectorChangingStation)

		;?kit is held by ?endeffector
		(kit-has-physicalLocation-refObject-endEffector ?kit - Kit ?endeffector - EndEffector)

		;?robot is equipped with ?endeffector
		(robot-has-endEffector ?robot - Robot ?endeffector - EndEffector)
	);End of the predicates section
	(:functions
		;capacity of kits that ?lbwk can have
		(capacity-of-kits-in-lbwk ?largeboxwithkits - LargeBoxWithKits)

		;quantity of kits in ?lbwk
		(quantity-of-kits-in-lbwk ?largeboxwithkits - LargeBoxWithKits)

		;flag set to 0 when a part is not found and to 1 when a part is found
		(part-found-flag)

		;flag set to 1 when a slot is not found and to 0 when a slot is found (same as above?)
		(slot-found-flag)
		
		;flag set to 1 when grasp is set and to 0 when not
		(grasp-set-flag)

		;capacity of parts of a certain ?sku that ?kit can have
		(capacity-of-parts-in-kit ?stockkeepingunit - StockKeepingUnit ?kit - Kit)

		;current quantity of parts in ?kit
		(current-quantity-of-parts-in-kit ?kit - Kit)

		;quantity of parts in ?partsTray
		(quantity-of-parts-in-partstray ?partstray - PartsTray)

		;quantity of kit trays in ?lbwekt
		(quantity-of-kittrays-in-lbwekt ?largeboxwithemptykittrays - LargeBoxWithEmptyKitTrays)

		;quantity of parts with ?sku in ?kit
		(quantity-of-parts-in-kit ?stockkeepingunit - StockKeepingUnit ?kit - Kit)

		;final quantity of parts in ?kit
		(final-quantity-of-parts-in-kit ?kit - Kit)
	);End of the functions section

	(:action detach-endEffector
		:parameters(
			?robot - Robot
			?endeffector - EndEffector
			?endeffectorholder - EndEffectorHolder
			?endeffectorchangingstation - EndEffectorChangingStation)
		:precondition(and
			(endEffector-has-no-heldObject ?endeffector)
			(endEffectorChangingStation-has-endEffectorHolder ?endeffectorchangingstation ?endeffectorholder)
			(endEffectorHolder-has-physicalLocation-refObject-changingStation ?endeffectorholder ?endeffectorchangingstation)
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot)
			(robot-has-endEffector ?robot ?endeffector))
		:effect(and
			(endEffectorHolder-has-endEffector ?endeffectorholder ?endeffector)
			(not(robot-has-endEffector ?robot ?endeffector))
			(endEffector-has-physicalLocation-refObject-endEffectorHolder ?endeffector ?endeffectorholder)
			(robot-has-no-endEffector ?robot)
			(not(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot)))
	)
	(:action take-part
		:parameters(
			?robot - Robot
			?part - Part
			?stockkeepingunit - StockKeepingUnit
			?partstray - PartsTray
			?endeffector - EndEffector
			?kit - Kit)
		:precondition(and
			(> (quantity-of-parts-in-partstray ?partstray) 0.000000)
			(< (grasp-set-flag) 1.0)
			(endEffector-is-for-partSKU ?endeffector ?stockkeepingunit)
			(endEffector-has-no-heldObject ?endeffector)
			(part-is-found ?part)
			(kit-exists ?kit)
			(robot-has-endEffector ?robot ?endeffector)
			(part-has-physicalLocation-refObject-partsTray ?part ?partstray)
			(partsVessel-has-part ?partstray ?part)
			(part-has-skuObject-sku ?part ?stockkeepingunit)
			(grasp-is-set ?stockkeepingunit)
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot))
		:effect(and
			(decrease (quantity-of-parts-in-partstray ?partstray) 1)
			(not(endEffector-has-no-heldObject ?endeffector))
			(increase (slot-found-flag) 1)
			(part-has-physicalLocation-refObject-endEffector ?part ?endeffector)
			(not(part-has-physicalLocation-refObject-partsTray ?part ?partstray))
			(endEffector-has-heldObject-part ?endeffector ?part))
	)
	(:action take-kitTray
		:parameters(
			?robot - Robot
			?kittray - KitTray
			?largeboxwithemptykittrays - LargeBoxWithEmptyKitTrays
			?endeffector - EndEffector
			?stockkeepingunit - StockKeepingUnit)
		:precondition(and
			(> (quantity-of-kittrays-in-lbwekt ?largeboxwithemptykittrays) 0.000000)
			(endEffector-is-for-kitTraySKU ?endeffector ?stockkeepingunit)
			(robot-has-endEffector ?robot ?endeffector)
			(endEffector-has-no-heldObject ?endeffector)
			(kitTray-has-physicalLocation-refObject-lbwekt ?kittray ?largeboxwithemptykittrays)
			(kitTray-has-skuObject-sku ?kittray ?stockkeepingunit)
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot))
		:effect(and
			(decrease (quantity-of-kittrays-in-lbwekt ?largeboxwithemptykittrays) 1)
			(not(kitTray-has-physicalLocation-refObject-lbwekt ?kittray ?largeboxwithemptykittrays))
			(kitTray-has-physicalLocation-refObject-endEffector ?kittray ?endeffector)
			(endEffector-has-heldObject-kitTray ?endeffector ?kittray)
			(not(endEffector-has-no-heldObject ?endeffector)))
	)
	(:action place-kitTray
		:parameters(
			?robot - Robot
			?kittray - KitTray
			?worktable - WorkTable
			?endeffector - EndEffector)
		:precondition(and
			(robot-has-endEffector ?robot ?endeffector)
			(endEffector-has-heldObject-kitTray ?endeffector ?kittray)
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot)
			(kitTray-has-physicalLocation-refObject-endEffector ?kittray ?endeffector))
		:effect(and
			(kitTray-has-physicalLocation-refObject-workTable ?kittray ?worktable)
			(not(kitTray-has-physicalLocation-refObject-endEffector ?kittray ?endeffector))
			(not(endEffector-has-heldObject-kitTray ?endeffector ?kittray))
			(endEffector-has-no-heldObject ?endeffector)
			(workTable-has-objectOnTable-kitTray ?worktable ?kittray))
	)
	(:action create-kit
		:parameters(
			?kit - Kit
			?kittray - KitTray
			?worktable - WorkTable)
		:precondition(and
			(kit-has-kitTray ?kit ?kittray)
			(workTable-has-objectOnTable-kitTray ?worktable ?kittray)
			(kitTray-has-physicalLocation-refObject-kit ?kittray ?kit)
			(kitTray-has-physicalLocation-refObject-workTable ?kittray ?worktable))
		:effect(and
			(not(workTable-has-objectOnTable-kitTray ?worktable ?kittray))
			(workTable-has-objectOnTable-kit ?worktable ?kit)
			(kit-has-physicalLocation-refObject-workTable ?kit ?worktable)
			(not(kitTray-has-physicalLocation-refObject-workTable ?kittray ?worktable))
			(kit-exists ?kit))
	)
	(:action attach-endEffector
		:parameters(
			?robot - Robot
			?endeffector - EndEffector
			?endeffectorholder - EndEffectorHolder
			?endeffectorchangingstation - EndEffectorChangingStation)
		:precondition(and
			(endEffectorHolder-has-endEffector ?endeffectorholder ?endeffector)
			(endEffectorChangingStation-has-endEffectorHolder ?endeffectorchangingstation ?endeffectorholder)
			(endEffectorHolder-has-physicalLocation-refObject-changingStation ?endeffectorholder ?endeffectorchangingstation)
			(endEffector-has-physicalLocation-refObject-endEffectorHolder ?endeffector ?endeffectorholder)
			(robot-has-no-endEffector ?robot))
		:effect(and
			(not(endEffector-has-physicalLocation-refObject-endEffectorHolder ?endeffector ?endeffectorholder))
			(not(endEffectorHolder-has-endEffector ?endeffectorholder ?endeffector))
			(not(robot-has-no-endEffector ?robot))
			(robot-has-endEffector ?robot ?endeffector)
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot)
			(endEffector-has-no-heldObject ?endeffector))
	)
	(:action place-kit
		:parameters(
			?robot - Robot
			?kit - Kit
			?endeffector - EndEffector
			?largeboxwithkits - LargeBoxWithKits)
		:precondition(and
			(< (quantity-of-kits-in-lbwk ?largeboxwithkits)  (capacity-of-kits-in-lbwk ?largeboxwithkits) )
			(kit-has-physicalLocation-refObject-endEffector ?kit ?endeffector)
			(robot-has-endEffector ?robot ?endeffector)
			(endEffector-has-heldObject-kit ?endeffector ?kit)
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot))
		:effect(and
			(increase (quantity-of-kits-in-lbwk ?largeboxwithkits) 1)
			(not(endEffector-has-heldObject-kit ?endeffector ?kit))
			(endEffector-has-no-heldObject ?endeffector)
			(not(kit-exists ?kit))
			(kit-has-physicalLocation-refObject-lbwk ?kit ?largeboxwithkits)
			(not(kit-has-physicalLocation-refObject-endEffector ?kit ?endeffector))
			(lbwk-has-kit ?largeboxwithkits ?kit))
	)
	(:action place-part
		:parameters(
			?robot - Robot
			?part - Part
			?stockkeepingunit - StockKeepingUnit
			?kit - Kit
			?endeffector - EndEffector
			?worktable - WorkTable
			?partstray - PartsTray)
		:precondition(and
			(< (quantity-of-parts-in-kit ?stockkeepingunit ?kit)  (capacity-of-parts-in-kit ?stockkeepingunit ?kit) )
			(part-has-skuObject-sku ?part ?stockkeepingunit)
			(part-has-physicalLocation-refObject-endEffector ?part ?endeffector)
			(endEffector-has-heldObject-part ?endeffector ?part)
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot)
			(robot-has-endEffector ?robot ?endeffector)
			(slot-is-found ?stockkeepingunit)
			(kit-exists ?kit))
		:effect(and
			(increase (current-quantity-of-parts-in-kit ?kit) 1)
			(increase (quantity-of-parts-in-kit ?stockkeepingunit ?kit) 1)
			(increase (part-found-flag) 1)
			(not(endEffector-has-heldObject-part ?endeffector ?part))
			(not(part-has-physicalLocation-refObject-endEffector ?part ?endeffector))
			(part-has-physicalLocation-refObject-kit ?part ?kit)
			(endEffector-has-no-heldObject ?endeffector))
	)
	(:action look-for-part
		:parameters(
			?robot - Robot
			?part - Part
			?stockkeepingunit - StockKeepingUnit
			?kit - Kit
			?endeffector - EndEffector)
		:precondition(and
			(> (part-found-flag) 0.000000)
			(endEffector-is-for-partSKU ?endeffector ?stockkeepingunit)
			(endEffector-has-no-heldObject ?endeffector)
			(part-has-skuObject-sku ?part ?stockkeepingunit)
			(kit-exists ?kit)
			(robot-has-endEffector ?robot ?endeffector)
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot))
		:effect(and
			(increase (grasp-set-flag) 1)
			(decrease (part-found-flag) 1)
			(part-is-found ?part))
	)
	(:action set-grasp
		 :parameters(
			?robot - Robot
			?part - Part
			?stockkeepingunit -StockKeepingUnit
			?endeffector - EndEffector)
		:precondition(and
			(> (grasp-set-flag) 0.0)
			(part-has-skuObject-sku ?part ?stockkeepingunit)
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot))
		:effect(and
			(decrease (grasp-set-flag) 1)
			(grasp-is-set ?stockkeepingunit))
	)
	(:action look-for-slot
		:parameters(
			?robot - Robot
			?part - Part
			?stockkeepingunit - StockKeepingUnit
			?kit - Kit
			?endeffector - EndEffector)
		:precondition(and
			(> (slot-found-flag) 0.000000)
			(endEffector-has-heldObject-part ?endeffector ?part)
			(part-has-skuObject-sku ?part ?stockkeepingunit)
			(kit-exists ?kit)
			(robot-has-endEffector ?robot ?endeffector)
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot))
		:effect(and
			(decrease (slot-found-flag) 1)
			(slot-is-found ?stockkeepingunit))
	)

	(:action take-kit
		:parameters(
			?robot - Robot
			?kit - Kit
			?kittray - KitTray
			?worktable - WorkTable
			?stockkeepingunit - StockKeepingUnit
			?endeffector - EndEffector)
		:precondition(and
			(= (current-quantity-of-parts-in-kit ?kit)  (final-quantity-of-parts-in-kit ?kit) )
			(endEffector-has-physicalLocation-refObject-robot ?endeffector ?robot)
			(kit-has-kitTray ?kit ?kittray)
			(endEffector-is-for-kitTraySKU ?endeffector ?stockkeepingunit)
			(robot-has-endEffector ?robot ?endeffector)
			(kitTray-has-skuObject-sku ?kittray ?stockkeepingunit)
			(endEffector-has-no-heldObject ?endeffector)
			(kit-exists ?kit))
		:effect(and
			(not(workTable-has-objectOnTable-kit ?worktable ?kit))
			(not(kit-has-physicalLocation-refObject-workTable ?kit ?worktable))
			(not(endEffector-has-no-heldObject ?endeffector))
			(endEffector-has-heldObject-kit ?endeffector ?kit)
			(kit-has-physicalLocation-refObject-endEffector ?kit ?endeffector))
	)
)
