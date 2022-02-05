/***************************************************************************
 *                (C) Copyright 2003-2022 - Faiumoni e. V.                 *
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Affero General Public License as        *
 *   published by the Free Software Foundation; either version 3 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 ***************************************************************************/

import { ui } from "../UI";

import { ActionContextMenu } from "../dialog/ActionContextMenu";
import { DropQuantitySelectorDialog } from "../dialog/DropQuantitySelectorDialog";

declare var marauroa: any;
declare var stendhal: any;

/**
 * a container for items like a bag or corpse
 */
export class ItemContainerImplementation {
	private rightClickDuration = 300;
	private timestampMouseDown = 0;
	private timestampMouseDownPrev = 0;
	private longTouchDuration = 300;
	private timestampTouchStart = 0;
	private timestampTouchEnd = 0;
	private touchDragActive = false;
	private dragData: DataTransfer|null = null;


	// TODO: replace usage of global document.getElementById()

	/**
	 * slot name, slot size, object (a corpse or chest) or null for marauroa.me,
	 * which changes on zone change.
	 */
	constructor(private slot: string, private size: number, private object: any, private suffix: string, private quickPickup: boolean, private defaultImage?: string) {
		for (let i = 0; i < size; i++) {
			let e = document.getElementById(slot + suffix + i)!;
			e.setAttribute("draggable", "true");
			e.addEventListener("dragstart", (event: DragEvent) => {
				this.onDragStart(event)
			});
			e.addEventListener("dragover", (event: DragEvent) => {
				this.onDragOver(event)
			});
			e.addEventListener("drop", (event: DragEvent) => {
				this.onDrop(event)
			});
			e.addEventListener("mousedown", (event: MouseEvent) => {
				this.onMouseDown(event)
			});
			e.addEventListener("mouseup", (event: MouseEvent) => {
				this.onMouseUp(event)
			});
			e.addEventListener("touchstart", (event: TouchEvent) => {
				this.onTouchStart(event)
			});
			e.addEventListener("touchend", (event: TouchEvent) => {
				this.onTouchEnd(event)
			});
			e.addEventListener("touchmove", (event: TouchEvent) => {
				this.onTouchMove(event)
			});
			e.addEventListener("touchcancel", (event: TouchEvent) => {
				this.onTouchCancel(event)
			});
			e.addEventListener("contextmenu", (event: MouseEvent) => {
				this.onContextMenu(event)
			});
		}
		this.update();
	}

	public update() {
		this.render();
	}

	public render() {
		let myobject = this.object || marauroa.me;
		let cnt = 0;
		if (myobject && myobject[this.slot]) {
			for (let i = 0; i < myobject[this.slot].count(); i++) {
				let o = myobject[this.slot].getByIndex(i);
				let e = document.getElementById(this.slot + this.suffix + cnt)!;
				e.style.backgroundImage = "url(/data/sprites/items/" + o["class"] + "/" + o["subclass"] + ".png " + ")";
				e.textContent = o.formatQuantity();
				(e as any).dataItem = o;
				cnt++;
			}
		}

		for (let i = cnt; i < this.size; i++) {
			let e = document.getElementById(this.slot +this. suffix + i)!;
			if (this.defaultImage) {
				e.style.backgroundImage = "url(/data/gui/" + this.defaultImage + ")";
			} else {
				e.style.backgroundImage = "none";
			}
			e.textContent = "";
			(e as any).dataItem = undefined;
		}
	}

	private onDragStart(event: DragEvent) {
		let myobject = this.object || marauroa.me;
		if (!myobject[this.slot]) {
			event.preventDefault();
			return;
		}

		let slotNumber = (event.target as HTMLElement).id.slice(this.slot.length + this.suffix.length);
		let item = myobject[this.slot].getByIndex(slotNumber);
		if (item) {
			let img = stendhal.data.sprites.getAreaOf(stendhal.data.sprites.get(item.sprite.filename), 32, 32);
			event.dataTransfer!.setDragImage(img, 0, 0);
			event.dataTransfer!.setData("Text", JSON.stringify({
				path: item.getIdPath(),
				zone: marauroa.currentZoneName
			}));
		} else {
			event.preventDefault();
		}
	}

	private onDragOver(event: DragEvent) {
		event.preventDefault();
		event.dataTransfer!.dropEffect = "move";
		return false;
	}

	private onDrop(event: DragEvent) {
		let myobject = this.object || marauroa.me;
		let datastr = event.dataTransfer?.getData("Text") || event.dataTransfer?.getData("text/x-stendhal");
		if (datastr) {
			let data = JSON.parse(datastr);
			let targetPath = "[" + myobject["id"] + "\t" + this.slot + "]";
			let action = {
				"type": "equip",
				"source_path": data.path,
				"target_path": targetPath,
				"zone" : data.zone
			};
			// if ctrl is pressed, we ask for the quantity
			if (event.ctrlKey) {
				ui.createSingletonFloatingWindow("Quantity",
					new DropQuantitySelectorDialog(action),
					event.pageX - 50, event.pageY - 25);
			} else {
				marauroa.clientFramework.sendAction(action);
			}
		}
		event.stopPropagation();
		event.preventDefault();
	}

	private onContextMenu(event: MouseEvent) {
		event.preventDefault();
	}

	isRightClick(event: MouseEvent) {
		if (event.which) {
			return (event.which === 3);
		} else {
			return (event.button === 2);
		}
	}

	isDoubleClick(evt: MouseEvent) {
		return (this.timestampMouseDown - this.timestampMouseDownPrev <= this.rightClickDuration);
	}

	private isLongTouch() {
		return (!this.touchDragActive
			&& this.timestampTouchEnd - this.timestampTouchStart > this.longTouchDuration);
	}

	onMouseDown(event: MouseEvent|TouchEvent) {
		this.timestampMouseDownPrev = this.timestampMouseDown;
		this.timestampMouseDown = +new Date();
	}

	onMouseUp(evt: MouseEvent|TouchEvent) {
		if (evt.type !== "touchend") {
			evt.preventDefault();
		}
		let event = stendhal.ui.html.extractPosition(evt);
		if ((event.target as any).dataItem) {
			if (this.quickPickup) {
				marauroa.clientFramework.sendAction({
					type: "equip",
					"source_path": (event.target as any).dataItem.getIdPath(),
					"target_path": "[" + marauroa.me["id"] + "\tbag]",
					"clicked": "", // useful for changing default target in equip action
					"zone": marauroa.currentZoneName
				});
				return;
			}

			if (this.isRightClick(event) || (evt.type === "touchend" && this.isLongTouch())) {
				ui.createSingletonFloatingWindow("Action",
					new ActionContextMenu((event.target as any).dataItem),
					event.pageX - 50, event.pageY - 5);
			//} else if (this.isDoubleClick(event)) {
			} else if (!this.touchDragActive) { // some players might like single click
				marauroa.clientFramework.sendAction({
					type: "use",
					"target_path": (event.target as any).dataItem.getIdPath(),
					"zone": marauroa.currentZoneName
				});
			}
		}
		document.getElementById("gamewindow")!.focus();
	}

	private getTouchDragData() {
		if (this.dragData === null) {
			this.dragData = new DataTransfer();
		}

		return this.dragData;
	}

	onTouchStart(evt: TouchEvent) {
		// FIXME: how to temporarily disable scrolling
		//evt.preventDefault();
		this.onMouseDown(evt);
		this.timestampTouchStart = this.timestampMouseDown;
	}

	onTouchEnd(evt: TouchEvent) {
		if (!this.touchDragActive) {
			this.timestampTouchEnd = +new Date();
			this.onMouseUp(evt);
		} else {
			this.touchDragActive = false;

			const touch = evt.touches[0];
			const eTarget = document.elementFromPoint(touch.pageX, touch.pageY);
			if (eTarget !== null) {
				eTarget.dispatchEvent(
					new DragEvent("drop", {dataTransfer: this.getTouchDragData()}));
			}
		}
	}

	onTouchMove(evt: TouchEvent) {
		const touch = evt.touches[0];
		const eTarget = document.elementFromPoint(touch.pageX, touch.pageY);

		if (!this.touchDragActive) {
			this.touchDragActive = true;

			if (eTarget !== null) {
				eTarget.dispatchEvent(
					new DragEvent("dragstart", {dataTransfer: this.getTouchDragData()}));
			}
		} else {
			if (eTarget !== null) {
				eTarget.dispatchEvent(
					new DragEvent("dragover", {dataTransfer: this.getTouchDragData()}));
			}
		}
	}

	onTouchCancel(evt: TouchEvent) {
		// DEBUG:
		console.log(evt.type + " event");
	}
}
