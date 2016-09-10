SELECT
	SUM(awake_time_entries.time - bedtime_time_entries.time)  AS total_sleep,
	SUM(awake_time_entries.time - bedtime_time_entries.time) / COUNT(*) AS average_sleep,
	COUNT(*) as sleep_count
	
FROM
	time_entries AS awake_time_entries
INNER JOIN
	time_entries AS bedtime_time_entries ON (
		awake_time_entries.center_of_day = (bedtime_time_entries.center_of_day + 86400000)   -- 'Today awake' matched to 'yesterday bedtime'
		AND bedtime_time_entries.time < awake_time_entries.time
	)
WHERE awake_time_entries.wakeup_or_bedtime = 'WAKE'
AND bedtime_time_entries.wakeup_or_bedtime = 'BEDTIME'
AND awake_time_entries.time_entry_type = 'TIME_RECORD'
AND bedtime_time_entries.time_entry_type = 'TIME_RECORD'
--AND, put in period limits here
--GROUP BY awake_time_entries.center_of_day