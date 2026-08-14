function resolveRegistrationAuditLoads(registrationMode) {
  const mode = String(registrationMode || '').toUpperCase()
  const isTeam = mode === 'TEAM'
  const isPersonal = mode === 'PERSONAL'
  return {
    loadTeamRegistration: isTeam,
    loadTeamRoster: isTeam,
    loadPersonalRegistration: isPersonal,
    loadFormation: isPersonal
  }
}

function assert(name, actual, expected) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected)
  if (!ok) {
    console.error('FAIL', name, actual, expected)
    process.exitCode = 1
    return
  }
  console.log('PASS', name)
}

assert('TEAM loads registration+roster only', resolveRegistrationAuditLoads('TEAM'), {
  loadTeamRegistration: true,
  loadTeamRoster: true,
  loadPersonalRegistration: false,
  loadFormation: false
})

assert('PERSONAL loads personal+formation', resolveRegistrationAuditLoads('PERSONAL'), {
  loadTeamRegistration: false,
  loadTeamRoster: false,
  loadPersonalRegistration: true,
  loadFormation: true
})

assert('unknown mode loads nothing mode-specific', resolveRegistrationAuditLoads(''), {
  loadTeamRegistration: false,
  loadTeamRoster: false,
  loadPersonalRegistration: false,
  loadFormation: false
})

assert('series-like code does not enable formation', resolveRegistrationAuditLoads('ARENA'), {
  loadTeamRegistration: false,
  loadTeamRoster: false,
  loadPersonalRegistration: false,
  loadFormation: false
})

if (process.exitCode) {
  console.error('registration audit load matrix failed')
  process.exit(1)
}
console.log('registration audit load matrix OK')