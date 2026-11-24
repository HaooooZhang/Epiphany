const r4Generation = 'r4-a'
const r4Cancel = true
const r4Scripted = Epiphany.id('epiphany_r4', 'scripted')

PlayerEvents.loggedIn(event => {
  const player = event.player
  console.info(`R4 ${r4Generation} binding ${player.username} points=${Epiphany.getInsightPoints(player)}`)
})

EpiphanyEvents.moduleSelect(event => {
  if (String(event.moduleId) !== String(r4Scripted)) return
  console.info(`R4 ${r4Generation} select-pre points=${Epiphany.getInsightPoints(event.player)}`)
  if (r4Cancel) event.cancel()
})

EpiphanyEvents.moduleSelected(event => {
  if (String(event.moduleId) !== String(r4Scripted)) return
  console.info(`R4 ${r4Generation} select-post points=${Epiphany.getInsightPoints(event.player)} stage=${event.player.stages.has('r4_stage')}`)
})

EpiphanyEvents.moduleComplete(event => {
  if (String(event.moduleId) !== String(r4Scripted)) return
  console.info(`R4 ${r4Generation} complete-pre`)
  if (r4Generation === 'r4-b') event.cancel()
})

EpiphanyEvents.moduleCompleted(event => {
  if (String(event.moduleId) !== String(r4Scripted)) return
  console.info(`R4 ${r4Generation} complete-post`)
})

EpiphanyEvents.aptitudeChanged(event => {
  console.info(`R4 ${r4Generation} aptitude ${event.oldAptitude} -> ${event.newAptitude}`)
})

EpiphanyEvents.epiphanySelected(event => {
  if (String(event.epiphanyId) !== 'epiphany_r4:stage') return
  console.info(`R4 ${r4Generation} epiphany-post insight-stage=${event.player.stages.has('r4_insight')}`)
})

ServerEvents.customCommand('r4_stage_check', event => {
  const player = event.entity
  console.info(`R4 ${r4Generation} stage-check insight=${player.stages.has('r4_insight')} module=${player.stages.has('r4_stage')}`)
})

ServerEvents.customCommand('r4_binding_check', event => {
  const player = event.entity
  const locked = Epiphany.id('epiphany_r4', 'chapter_complete')
  Epiphany.addAptitude(player, 1)
  Epiphany.moduleSelect(player, r4Scripted)
  Epiphany.moduleForceSelect(player, locked)
  console.info(`R4 ${r4Generation} binding-check aptitude=${Epiphany.getAptitude(player)} normal=${Epiphany.isModuleSelected(player, r4Scripted)} forced=${Epiphany.isModuleSelected(player, locked)}`)
})
