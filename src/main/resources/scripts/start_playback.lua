local indexKey = KEYS[1]
local subscriberId = ARGV[1]
local deviceId = ARGV[2]
local ttl = tonumber(ARGV[3])
local maxDevices = tonumber(ARGV[4])
local now = tonumber(ARGV[5])

local playbackKey = "playback:" .. subscriberId .. ":" .. deviceId

-- Step 1: read all devices
local devices = redis.call("SMEMBERS", indexKey)

-- Step 2: remove stale devices
local active = 0
for _, dev in ipairs(devices) do
    local key = "playback:" .. subscriberId .. ":" .. dev
    if redis.call("EXISTS", key) == 1 then
        active = active + 1
    else
        redis.call("SREM", indexKey, dev)
    end
end

-- Step 3: limit check
if active >= maxDevices then
    return {err = "LIMIT_REACHED"}
end

-- Step 4: register
redis.call("HMSET",
    playbackKey,
    "deviceId", deviceId,
    "subscriberId", subscriberId,
    "lastSeen", now)

redis.call("EXPIRE", playbackKey, ttl)
redis.call("SADD", indexKey, deviceId)

return "OK"
