/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

import ClayButton, {ClayButtonWithIcon} from '@clayui/button';
import {ClayDropDownWithItems} from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import ClayManagementToolbar from '@clayui/management-toolbar';
import {ReactNode, useContext} from 'react';

import {ListViewContext, ListViewTypes} from '../../context/ListViewContext';
import i18n from '../../i18n';
import {FilterSchema} from '../../schema/filter';
import ManagementToolbarFilter from './ManagementToolbarFilter';

export type IItem = {
	active?: boolean;
	checked?: boolean;
	disabled?: boolean;
	href?: string;
	items?: IItem[];
	label?: string;
	name?: string;
	onChange?: Function;
	onClick?: (event: React.MouseEvent<HTMLElement, MouseEvent>) => void;
	symbolLeft?: string;
	symbolRight?: string;
	type?:
		| 'checkbox'
		| 'contextual'
		| 'group'
		| 'item'
		| 'radio'
		| 'radiogroup'
		| 'divider';
	value?: string;
};

type ManagementToolbarRightProps = {
	actions: any;
	addButton?: () => void;
	buttons?: ReactNode;
	columns: IItem[];
	disabled: boolean;
	display?: {
		columns?: boolean;
	};
	filterSchema?: FilterSchema;
};

const ManagementToolbarRight: React.FC<ManagementToolbarRightProps> = ({
	actions,
	addButton,
	buttons,
	display = {columns: true},
	columns,
	disabled,
	filterSchema,
}) => {
	const [{pin}, dispatch] = useContext(ListViewContext);

	return (
		<ClayManagementToolbar.ItemList>
			{filterSchema?.fields?.length && (
				<>
					<ClayManagementToolbar.Item>
						<ClayButtonWithIcon
							aria-label={i18n.translate('add-pin')}
							className="nav-btn nav-btn-monospaced"
							displayType="unstyled"
							onClick={() =>
								dispatch({type: ListViewTypes.SET_PIN})
							}
							symbol={i18n.translate(pin ? 'unpin' : 'pin')}
							title={i18n.translate(pin ? 'unpin' : 'pin')}
						/>
					</ClayManagementToolbar.Item>

					<ManagementToolbarFilter filterSchema={filterSchema} />
				</>
			)}

			{display.columns && (
				<ClayDropDownWithItems
					items={columns}
					trigger={
						<ClayButton
							className="nav-link"
							disabled={disabled}
							displayType="unstyled"
						>
							<span className="navbar-breakpoint-down-d-none">
								<span
									className="navbar-text-truncate"
									title={i18n.translate('columns')}
								>
									<ClayIcon
										className="inline-item inline-item-after"
										symbol="columns"
									/>
								</span>
							</span>

							<span className="navbar-breakpoint-d-none">
								<ClayIcon symbol="columns" />
							</span>
						</ClayButton>
					}
				/>
			)}

			{buttons}

			{actions?.create && addButton && (
				<ClayManagementToolbar.Item
					className="ml-2"
					onClick={addButton}
				>
					<ClayButtonWithIcon
						className="nav-btn nav-btn-monospaced"
						symbol="plus"
					/>
				</ClayManagementToolbar.Item>
			)}
		</ClayManagementToolbar.ItemList>
	);
};

export default ManagementToolbarRight;
