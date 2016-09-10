SELECT
	 SUM(records.time - 
	 (CASE WHEN custom_targets.time IS null THEN (records.center_of_day + targets.time)
	 ELSE (custom_targets.time) END
	 )) 
	 / COUNT(*) AS average_offset_time
FROM 
	time_entries as records
CROSS JOIN
	time_entries as targets ON (
		case cast (strftime('%w', records.time/1000, 'unixepoch') as integer)  -- Get dayname of record
		when 0 then 'Sunday'
		when 1 then 'Monday'
		when 2 then 'Tuesday'
		when 3 then 'Wednesday'
		when 4 then 'Thursday'
		when 5 then 'Friday'
		else 'Saturday' end || '_default' 
		= targets.time_entry_type COLLATE NOCASE  -- Cross join matching default target
		AND records.wakeup_or_bedtime = targets.wakeup_or_bedtime
		)
LEFT OUTER JOIN 
	time_entries AS custom_targets ON (
		records.center_of_day = custom_targets.center_of_day -- Outer join matching custom targets
		AND custom_targets.time_entry_type = 'TIME_TARGET'
		AND records.wakeup_or_bedtime = custom_targets.wakeup_or_bedtime
		)
WHERE
	records.time_entry_type = 'TIME_RECORD'
	AND records.wakeup_or_bedtime = 'WAKE'