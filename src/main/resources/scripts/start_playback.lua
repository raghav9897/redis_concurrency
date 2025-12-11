-- start_playback.lua
-- KEYS[1] = key
-- ARGV[1] = nowMillis
-- ARGV[2] = timeoutMillis
-- ARGV[3] = sessionId
-- ARGV[4] = sessionJson
-- ARGV[5] = maxStreams


local key = KEYS[1]
local now = tonumber(ARGV[1])
local timeout = tonumber(ARGV[2])
local sessionId = ARGV[3]
local sessionJson = ARGV[4]
local maxStreams = tonumber(ARGV[5])


local fields = redis.call('HKEYS', key)
local active = 0


for i=1,#fields do
local sid = fields[i]
local val = redis.call('HGET', key, sid)
if val then
local ok, obj = pcall(cjson.decode, val)
if ok and obj["lastPing"] then
if (now - obj["lastPing"]) < timeout then
active = active + 1
else
redis.call('HDEL', key, sid)
end
else
redis.call('HDEL', key, sid)
end
end
end


if active >= maxStreams then
return -1
end


redis.call('HSET', key, sessionId, sessionJson)
return 1