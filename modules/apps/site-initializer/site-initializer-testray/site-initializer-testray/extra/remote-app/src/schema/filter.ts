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

import {RendererFields} from '../components/Form/Renderer';
import i18n from '../i18n';
import {
	TestrayCaseType,
	TestrayComponent,
	TestrayProductVersion,
	TestrayProject,
	TestrayRoutine,
	TestrayRun,
	TestrayTeam,
	UserAccount,
} from '../services/rest';
import {SearchBuilder} from '../util/search';
import {CaseResultStatuses, TaskStatuses} from '../util/statuses';

export type Filters = {
	[key: string]: RendererFields[];
};

type Filter = {
	[key: string]: RendererFields;
};

export type FilterVariables = {
	appliedFilter: {
		[key: string]: string;
	};
	defaultFilter: string | SearchBuilder;
	filterSchema: FilterSchema;
};

export type FilterSchema = {
	fields: RendererFields[];
	name?: string;
	onApply?: (filterVariables: FilterVariables) => string;
};

export type FilterSchemas = {
	[key: string]: FilterSchema;
};

export type FilterSchemaOption = keyof typeof filterSchema;

const transformData = <T = any>(response: any): T[] => {
	return response?.items || [];
};

const dataToOptions = <T = any>(
	entries: T[],
	transformAction?: (entry: T) => {label: string; value: number | string}
) =>
	entries.map((entry: any) =>
		transformAction
			? transformAction(entry)
			: {label: entry.name, value: entry.id}
	);

const baseFilters: Filter = {
	assignee: {
		label: i18n.translate('assignee'),
		name: 'assignedUsers',
		resource: '/user-accounts',
		transformData(item) {
			return dataToOptions(
				transformData<UserAccount>(item),
				(userAccount) => ({
					label: `${userAccount.givenName} ${userAccount.additionalName}`,
					value: userAccount.givenName,
				})
			);
		},
		type: 'select',
	},
	caseType: {
		label: i18n.translate('case-type'),
		name: 'caseType',
		resource: '/casetypes?fields=id,name&sort=name:asc&pageSize=100',
		transformData(item) {
			return dataToOptions(transformData<TestrayCaseType>(item));
		},
		type: 'multiselect',
	},
	component: {
		label: i18n.translate('component'),
		name: 'componentId',
		resource: ({projectId}) =>
			`/components?fields=id,name&sort=name:asc&pageSize=200&filter=${SearchBuilder.eq(
				'projectId',
				projectId as string
			)}`,
		transformData(item) {
			return dataToOptions(transformData<TestrayComponent>(item));
		},
		type: 'select',
	},
	description: {
		label: i18n.translate('description'),
		name: 'description',
		type: 'textarea',
	},
	dueStatus: {
		label: i18n.translate('status'),
		name: 'dueStatus',
		type: 'checkbox',
	},
	erros: {
		label: i18n.translate('errors'),
		name: 'errors',
		type: 'textarea',
	},
	hasRequirements: {
		disabled: true,
		label: i18n.translate('has-requirements'),
		name: 'caseToRequirementsCases',
		options: ['true', 'false'],
		type: 'select',
	},
	issues: {
		label: i18n.translate('issues'),
		name: 'issues',
		type: 'textarea',
	},
	priority: {
		label: i18n.translate('priority'),
		name: 'priority',
		options: ['1', '2', '3', '4', '5'],
		type: 'multiselect',
	},
	productVersion: {
		label: i18n.translate('product-version'),
		name: 'productVersion',
		resource: ({projectId}) =>
			`/productversions?fields=id,name&sort=name:asc&pageSize=100&filter=${SearchBuilder.eq(
				'projectId',
				projectId as string
			)}`,
		transformData(item) {
			return dataToOptions(transformData<TestrayProductVersion>(item));
		},
		type: 'select',
	},
	project: {
		label: i18n.translate('project'),
		name: 'projectId',
		resource: '/projects?fields=id,name',
		transformData(item) {
			return dataToOptions(transformData<TestrayProject>(item));
		},
		type: 'select',
	},
	routine: {
		label: i18n.translate('routines'),
		name: 'routines',
		resource: ({projectId}) =>
			`/routines?fields=id,name&pageSize=100&filter=${SearchBuilder.eq(
				'projectId',
				projectId as string
			)}`,
		transformData(item) {
			return dataToOptions(transformData<TestrayRoutine>(item));
		},
		type: 'select',
	},
	run: {
		label: i18n.translate('run'),
		name: 'run',
		resource: '/runs?fields=id,name',
		transformData(item) {
			return dataToOptions(transformData<TestrayRun>(item));
		},
		type: 'select',
	},
	steps: {
		label: i18n.translate('steps'),
		name: 'steps',
		type: 'textarea',
	},
	team: {
		label: i18n.translate('team'),
		name: 'teamId',
		options: [{label: 'Solutions', value: 'solutions'}],
		resource: ({projectId}) =>
			`/teams?fields=id,name&sort=name:asc&pageSize=100&filter=${SearchBuilder.eq(
				'projectId',
				projectId as string
			)}`,
		transformData(item) {
			return dataToOptions(transformData<TestrayTeam>(item));
		},
		type: 'select',
	},
};

const overrides = (
	object: RendererFields,
	newObject: Partial<RendererFields>
) => ({
	...object,
	...newObject,
});

const filterSchema = {
	buildCaseTypes: {
		fields: [baseFilters.priority, baseFilters.team] as RendererFields[],
	},
	buildComponents: {
		fields: [
			baseFilters.priority,
			overrides(baseFilters.caseType, {disabled: false}),
			baseFilters.team,
			baseFilters.run,
		] as RendererFields[],
	},
	buildResults: {
		fields: [
			overrides(baseFilters.caseType, {
				name: 'caseToCaseResult/r_caseTypeToCases_c_caseTypeId',
				type: 'multiselect',
			}),
			overrides(baseFilters.priority, {
				name: 'caseToCaseResult/priority',
				removeQuoteMark: true,
				type: 'select',
			}),
			overrides(baseFilters.team, {
				name: 'componentToCaseResult/r_teamToComponents_c_teamId',
				type: 'multiselect',
			}),
			overrides(baseFilters.component, {
				name: 'componentToCaseResult/id',
				type: 'multiselect',
			}),
			{
				label: i18n.translate('environment'),
				name: 'runToCaseResult/name',
				operator: 'contains',
				type: 'text',
			},
			overrides(baseFilters.run, {name: 'runToCaseResult/id'}),
			{
				label: i18n.translate('case-name'),
				name: 'caseToCaseResult/name',
				operator: 'contains',
				type: 'text',
			},
			overrides(baseFilters.assignee, {name: 'userId'}),
			overrides(baseFilters.dueStatus, {
				options: [
					{
						label: 'Blocked',
						value: CaseResultStatuses.BLOCKED,
					},
					{
						label: 'Failed',
						value: CaseResultStatuses.FAILED,
					},
					{
						label: 'In Progress',
						value: CaseResultStatuses.IN_PROGRESS,
					},
					{
						label: 'Passed',
						value: CaseResultStatuses.PASSED,
					},
					{
						label: 'Test Fix',
						value: CaseResultStatuses.TEST_FIX,
					},
					{
						label: 'Untested',
						value: CaseResultStatuses.UNTESTED,
					},
				],
			}),
			overrides(baseFilters.issues, {
				operator: 'contains',
			}),
			overrides(baseFilters.erros, {
				operator: 'contains',
			}),
			{
				label: i18n.translate('comments'),
				name: 'comment',
				operator: 'contains',
				type: 'textarea',
			},
		] as RendererFields[],
	},
	buildResultsHistory: {
		fields: [
			overrides(baseFilters.productVersion, {
				label: i18n.translate('product-version-name'),
				name:
					'buildToCaseResult/r_productVersionToBuilds_c_productVersionId',
				type: 'multiselect',
			}),
			{
				label: i18n.translate('environment'),
				name: 'runToCaseResult/name',
				operator: 'contains',
				type: 'text',
			},
			overrides(baseFilters.routine, {
				name: 'buildToCaseResult/routineId',
			}),
			overrides(baseFilters.assignee, {
				name: 'userId',
			}),
			overrides(baseFilters.dueStatus, {
				options: [
					{
						label: 'Blocked',
						value: CaseResultStatuses.BLOCKED,
					},
					{
						label: 'Failed',
						value: CaseResultStatuses.FAILED,
					},
					{
						label: 'In Progress',
						value: CaseResultStatuses.IN_PROGRESS,
					},
					{
						label: 'Passed',
						value: CaseResultStatuses.PASSED,
					},
					{
						label: 'Test Fix',
						value: CaseResultStatuses.TEST_FIX,
					},
					{
						label: 'Untested',
						value: CaseResultStatuses.UNTESTED,
					},
				],
			}),
			overrides(baseFilters.issues, {
				operator: 'contains',
			}),
			overrides(baseFilters.erros, {
				operator: 'contains',
			}),
			{
				label: i18n.translate('case-result-warning'),
				name: 'warnings',
				type: 'number',
			},
			{
				label: i18n.sub('x-create-date', 'min'),
				name: 'dateCreated',
				operator: 'gt',
				type: 'date',
			},
			{
				label: i18n.sub('x-create-date', 'max'),
				name: 'dateCreated$',
				operator: 'lt',
				type: 'date',
			},
			overrides(baseFilters.team, {
				name: 'componentToCaseResult/r_teamToComponents_c_teamId',
				type: 'multiselect',
			}),
		] as RendererFields[],
	},
	buildRuns: {
		fields: [
			baseFilters.priority,
			baseFilters.caseType,
			baseFilters.team,
		] as RendererFields[],
	},
	buildTeams: {
		fields: [
			baseFilters.priority,
			baseFilters.caseType,
			baseFilters.team,
			baseFilters.run,
		] as RendererFields[],
	},
	buildTemplates: {
		fields: [
			{
				label: i18n.translate('template-name'),
				name: 'template-name',
				type: 'text',
			},
			{
				label: i18n.translate('status'),
				name: 'status',
				type: 'select',
			},
		] as RendererFields[],
	},
	builds: {
		fields: [
			baseFilters.priority,
			baseFilters.productVersion,
			baseFilters.caseType,
			{
				label: i18n.translate('build-name'),
				name: 'buildName',
				type: 'text',
			},
			{
				label: i18n.translate('status'),
				name: 'status',
				options: ['Open', 'Abandoned', 'Complete', 'In Analysis'],
				type: 'checkbox',
			},
			baseFilters.team,
		] as RendererFields[],
	},
	cases: {
		fields: [
			overrides(baseFilters.priority, {
				removeQuoteMark: true,
				type: 'select',
			}),
			overrides(baseFilters.caseType, {
				name: 'r_caseTypeToCases_c_caseTypeId',
			}),
			{
				label: i18n.translate('case-name'),
				name: 'name',
				operator: 'contains',
				type: 'text',
			},
			overrides(baseFilters.team, {
				name: 'componentToCases/r_teamToComponents_c_teamId',
				type: 'multiselect',
			}),
			overrides(baseFilters.component, {
				name: 'componentId',
				type: 'multiselect',
			}),
			baseFilters.description,
			baseFilters.steps,
			baseFilters.issues,
			baseFilters.hasRequirements,
		] as RendererFields[],
	},
	requirementCases: {
		fields: [
			baseFilters.priority,
			baseFilters.caseType,
			{
				label: i18n.translate('case-name'),
				name: 'caseName',
				type: 'text',
			},
			baseFilters.team,
			{
				label: i18n.translate('component'),
				name: 'component',
				type: 'text',
			},
		] as RendererFields[],
	},
	requirements: {
		fields: [
			{
				label: i18n.translate('key'),
				name: 'key',
				operator: 'contains',
				type: 'text',
			},
			{
				label: i18n.translate('link'),
				name: 'linkURL',
				operator: 'contains',
				type: 'text',
			},
			overrides(baseFilters.team, {
				name: 'componentToRequirements/r_teamToComponents_c_teamId',
				type: 'multiselect',
			}),
			overrides(baseFilters.component, {type: 'multiselect'}),
			{
				label: i18n.translate('jira-components'),
				name: 'components',
				operator: 'contains',
				type: 'text',
			},
			{
				label: i18n.translate('summary'),
				name: 'summary',
				operator: 'contains',
				type: 'text',
			},
			{
				disabled: true,
				label: i18n.translate('case'),
				name: 'case',
				type: 'textarea',
			},
		] as RendererFields[],
	},
	routines: {
		fields: [
			baseFilters.priority,
			baseFilters.caseType,
			baseFilters.team,
		] as RendererFields[],
	},
	subtasks: {
		fields: [
			{
				label: i18n.translate('subtask-name'),
				name: 'subtaskName',
				type: 'text',
			},
			{
				label: i18n.translate('errors'),
				name: 'errors',
				type: 'text',
			},
			baseFilters.assignee,
			{
				label: i18n.translate('status'),
				name: 'status',
				options: ['Complete', 'In Analysis', 'Open'],
				type: 'checkbox',
			},
			baseFilters.team,
			{
				label: i18n.translate('component'),
				name: 'commponent',
				type: 'text',
			},
		] as RendererFields[],
	},
	suites: {
		fields: [
			{
				label: i18n.translate('suite-name'),
				name: 'suiteName',
				type: 'text',
			},
			{
				label: i18n.translate('description'),
				name: 'description',
				type: 'text',
			},
		] as RendererFields[],
	},
	teams: {
		fields: [
			{
				label: i18n.translate('team-name'),
				name: 'name',
				operator: 'contains',
				type: 'text',
			},
		] as RendererFields[],
	},
	testflow: {
		fields: [
			{
				label: i18n.sub('task-x', 'name'),
				name: 'name',
				operator: 'contains',
				type: 'text',
			},
			overrides(baseFilters.project, {
				label: i18n.translate('project-name'),
				name: 'buildToTasks/r_projectToBuilds_c_projectId',
				type: 'multiselect',
			}),
			overrides(baseFilters.routine, {
				label: i18n.translate('routine-name'),
				name: 'buildToTasks/r_routineToBuilds_c_routineId',
				resource: '/routines?fields=id,name&sort=name:asc&pageSize=100',
				type: 'multiselect',
			}),
			{
				label: i18n.translate('build-name'),
				name: 'buildToTasks/name',
				operator: 'contains',
				removeQuoteMark: false,
				type: 'text',
			},
			overrides(baseFilters.dueStatus, {
				options: [
					{
						label: 'Abandoned',
						value: TaskStatuses.ABANDONED,
					},
					{
						label: 'Complete',
						value: TaskStatuses.COMPLETE,
					},
					{
						label: 'In Analysis',
						value: TaskStatuses.IN_ANALYSIS,
					},
				],
			}),
			overrides(baseFilters.assignee, {
				operator: 'contains',
				type: 'select',
			}),
		] as RendererFields[],
	},
} as const;

export {filterSchema};
