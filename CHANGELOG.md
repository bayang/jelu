### [0.60.1](https://github.com/bayang/jelu/compare/v0.60.0...v0.60.1) (2024-10-26)


### Bug Fixes

* remove print statements ([afcaeb0](https://github.com/bayang/jelu/commit/afcaeb0aaa02100f27fdce1113e393b7f2b45abd))

## [0.60.0](https://github.com/bayang/jelu/compare/v0.59.2...v0.60.0) (2024-10-26)


### Features

* save activity in DB [#116](https://github.com/bayang/jelu/issues/116) ([baa86f8](https://github.com/bayang/jelu/commit/baa86f8d558539471ded029f70abd0408e63d40e))


### Bug Fixes

* image cache too strict [#147](https://github.com/bayang/jelu/issues/147) ([fb3477f](https://github.com/bayang/jelu/commit/fb3477f05584510301d2de4761536cca941fde53))

### [0.59.2](https://github.com/bayang/jelu/compare/v0.59.1...v0.59.2) (2024-10-07)


### Bug Fixes

* all books deleted from series [#146](https://github.com/bayang/jelu/issues/146) ([e02acf9](https://github.com/bayang/jelu/commit/e02acf9ba2eed6124a238f93e2857b24c6784aa1))

### [0.59.1](https://github.com/bayang/jelu/compare/v0.59.0...v0.59.1) (2024-09-23)


### Bug Fixes

* 404 on bulk edit [#143](https://github.com/bayang/jelu/issues/143) ([99b0ce4](https://github.com/bayang/jelu/commit/99b0ce4712c86db344ccd97c83b3549e160d7161))

## [0.59.0](https://github.com/bayang/jelu/compare/v0.58.0...v0.59.0) (2024-09-21)


### Features

* add series description and rating [#122](https://github.com/bayang/jelu/issues/122) ([f32d1fb](https://github.com/bayang/jelu/commit/f32d1fbc45e1f6cdf164b4f9b8e828911b722245))


### Bug Fixes

* series order number on card [#140](https://github.com/bayang/jelu/issues/140) ([c15ac02](https://github.com/bayang/jelu/commit/c15ac0299003d6239aaeacb9ab02ba9c4dfadf2a))

## [0.58.0](https://github.com/bayang/jelu/compare/v0.57.0...v0.58.0) (2024-09-15)


### Features

* also import reviews from goodreads [#132](https://github.com/bayang/jelu/issues/132) ([4ff2cd1](https://github.com/bayang/jelu/commit/4ff2cd1d1883d3045374aa71cb6e6cd04ef51269))


### Bug Fixes

* deleting quote or review not updating book detail page ([d9d7a63](https://github.com/bayang/jelu/commit/d9d7a632666dcb5b75040a364523b07cb7c63455))

## [0.57.0](https://github.com/bayang/jelu/compare/v0.56.3...v0.57.0) (2024-09-14)


### Features

* add book quotes [#124](https://github.com/bayang/jelu/issues/124) ([3dbb33d](https://github.com/bayang/jelu/commit/3dbb33d14c0352e19d9b9cfe66ec61ece647bc23))

### [0.56.3](https://github.com/bayang/jelu/compare/v0.56.2...v0.56.3) (2024-09-04)


### Bug Fixes

* series sort [#135](https://github.com/bayang/jelu/issues/135) ([2b52709](https://github.com/bayang/jelu/commit/2b527091f0ebe056c4b0aa7fe020c13ba84594e2))

### [0.56.2](https://github.com/bayang/jelu/compare/v0.56.1...v0.56.2) (2024-08-19)


### Bug Fixes

* autocomplete loses focus, upgrade oruga [#133](https://github.com/bayang/jelu/issues/133) ([f623512](https://github.com/bayang/jelu/commit/f623512085636a1cb4cf9b426e51da5d57f087bd))
* barcode reader issues [#126](https://github.com/bayang/jelu/issues/126) [#118](https://github.com/bayang/jelu/issues/118) ([ff6090b](https://github.com/bayang/jelu/commit/ff6090bb2251a516ea2a58a049625175e550696a))
* docker build issue ([34533af](https://github.com/bayang/jelu/commit/34533affd31fe337752a505000c08f226467819f))
* oruga css path ([6cabe7f](https://github.com/bayang/jelu/commit/6cabe7f29f84685962409d8fdaf49db0c1d5d4e8))
* ts compilation ([d8705c1](https://github.com/bayang/jelu/commit/d8705c1e5d8f4b026ab072109476216fb20835b5))

### [0.56.1](https://github.com/bayang/jelu/compare/v0.56.0...v0.56.1) (2024-05-03)


### Bug Fixes

* New Crowdin updates ([#119](https://github.com/bayang/jelu/issues/119)) ([e2f1ee3](https://github.com/bayang/jelu/commit/e2f1ee3a5efbae91585fef20c7aca9f37753670f))

## [0.56.0](https://github.com/bayang/jelu/compare/v0.55.0...v0.56.0) (2024-04-29)


### Features

* add Random page and random sorting option ([#109](https://github.com/bayang/jelu/issues/109)) ([186f8f0](https://github.com/bayang/jelu/commit/186f8f050db3eb63b64aa20d63faee768bc4b109))

## [0.55.0](https://github.com/bayang/jelu/compare/v0.54.0...v0.55.0) (2024-04-25)


### Features

* allow filtering by event on tags pages ([e2aba9a](https://github.com/bayang/jelu/commit/e2aba9a061e4aa07713fc40da0ab0f7d6a57c2ec))
* Allow typing into date input fields during book event editing. ([#112](https://github.com/bayang/jelu/issues/112)) ([0557fb4](https://github.com/bayang/jelu/commit/0557fb41be4b36f7a627934e6e824a16419eb669))
* detect if isbn field contains ASIN for metadata search [#113](https://github.com/bayang/jelu/issues/113) ([41d80f1](https://github.com/bayang/jelu/commit/41d80f1c614172262df48a87d9712810c6ab7e9c))


### Bug Fixes

* use a better maintained barcode reader lib ([39f7835](https://github.com/bayang/jelu/commit/39f78356c7e09ea8a30cc408d834df7c24fa7555))

## [0.54.0](https://github.com/bayang/jelu/compare/v0.53.0...v0.54.0) (2024-04-25)


### Features

* auto select latest year in stats views ([47a5526](https://github.com/bayang/jelu/commit/47a55263a647d8663ea5d42bb5929dcf00000d6b))
* sort last events on homepage by event end date [#115](https://github.com/bayang/jelu/issues/115) ([36e483e](https://github.com/bayang/jelu/commit/36e483e1f71dfd39b382c7e9cb252404e87d0253))


### Bug Fixes

* use a better maintained barcode reader lib ([dbd45d8](https://github.com/bayang/jelu/commit/dbd45d89b5c5ca3c6e7e9579cb8269e6d2af9158))
* use a better maintained barcode reader lib ([4ca9983](https://github.com/bayang/jelu/commit/4ca99831dc67b4e97005d4015fc1ff5eeb042892))
* use a better maintained barcode reader lib ([c923a30](https://github.com/bayang/jelu/commit/c923a30631298c9c37e0f142f9cb0af0d3c0c351))

## [0.53.0](https://github.com/bayang/jelu/compare/v0.52.2...v0.53.0) (2024-04-22)


### Features

* Allow typing into date input fields during book event editing. ([#112](https://github.com/bayang/jelu/issues/112)) ([c951a49](https://github.com/bayang/jelu/commit/c951a491ab8356b6ac1af9f735769e7534763aa9))

### [0.52.2](https://github.com/bayang/jelu/compare/v0.52.1...v0.52.2) (2024-04-20)


### Bug Fixes

* fix docker packaging ([ae5e316](https://github.com/bayang/jelu/commit/ae5e316334e3a313921ccd76a84d99f4d9ab69c9))

### [0.52.1](https://github.com/bayang/jelu/compare/v0.52.0...v0.52.1) (2024-04-20)


### Bug Fixes

* frontend dependencies upgrade issue ([78cf03b](https://github.com/bayang/jelu/commit/78cf03b2220cba64d25f2f33bf1a21ba02b2f701))

## [0.52.0](https://github.com/bayang/jelu/compare/v0.51.0...v0.52.0) (2024-04-20)


### Features

* link Series # tag on Book Cards ([#110](https://github.com/bayang/jelu/issues/110)) ([cb8acac](https://github.com/bayang/jelu/commit/cb8acac178527f6b1a4639516364f7353ad7263c))

## [0.51.0](https://github.com/bayang/jelu/compare/v0.50.0...v0.51.0) (2024-02-28)


### Features

* add a working PWA ([204af0b](https://github.com/bayang/jelu/commit/204af0b708f2798023f396051a29084a76406b71))

## [0.50.0](https://github.com/bayang/jelu/compare/v0.49.1...v0.50.0) (2024-02-11)


### Features

* allow user deletion [#102](https://github.com/bayang/jelu/issues/102) ([74c59e7](https://github.com/bayang/jelu/commit/74c59e7e011d51bad88280db8f65160b7e020842))


### Bug Fixes

* jelu theme cards ([5bf057d](https://github.com/bayang/jelu/commit/5bf057df25af92575f5abadb3fd2622ebf04148f))
* swagger not showing [#103](https://github.com/bayang/jelu/issues/103) ([f769593](https://github.com/bayang/jelu/commit/f769593ad4c950ac6a9ccda5a5a7f314f606d610))

### [0.49.1](https://github.com/bayang/jelu/compare/v0.49.0...v0.49.1) (2024-02-04)


### Bug Fixes

* personal notes maxLength in edit book form ([9514973](https://github.com/bayang/jelu/commit/9514973bc8aa24ddec929811e9e4fc53c6f2d5b2))

## [0.49.0](https://github.com/bayang/jelu/compare/v0.48.0...v0.49.0) (2024-01-14)


### Features

* display and sort by ratings [#94](https://github.com/bayang/jelu/issues/94) ([8e591d6](https://github.com/bayang/jelu/commit/8e591d68447d45c34e150cb875d6f577f119ab88))


### Bug Fixes

* authors spacing on cards [#95](https://github.com/bayang/jelu/issues/95) ([9634299](https://github.com/bayang/jelu/commit/9634299b706df469b184f3a8d037850000b8b66d))
* display page number or percentage on progress bar hovering ([d19570b](https://github.com/bayang/jelu/commit/d19570bcc4f7e2b6f41a136b676bf4aed59fa008))

## [0.48.0](https://github.com/bayang/jelu/compare/v0.47.0...v0.48.0) (2023-12-31)


### Features

* edit current progress and page ([e4402c6](https://github.com/bayang/jelu/commit/e4402c6a822673d54ea43dc8f3eb8fbdb13c7a52))


### Bug Fixes

* collapse dropdown menus after click [#92](https://github.com/bayang/jelu/issues/92) ([6cadfc9](https://github.com/bayang/jelu/commit/6cadfc946fce1688f849b8bdec7e3848153978ab))

## [0.47.0](https://github.com/bayang/jelu/compare/v0.46.1...v0.47.0) (2023-12-23)


### Features

* add sorting by page count [#89](https://github.com/bayang/jelu/issues/89) ([0a58a5a](https://github.com/bayang/jelu/commit/0a58a5a36aea7dc5cdbcc19ebc226b9c57fd6898))

### [0.46.1](https://github.com/bayang/jelu/compare/v0.46.0...v0.46.1) (2023-12-09)


### Bug Fixes

* bug in network image saving ([9d81fd9](https://github.com/bayang/jelu/commit/9d81fd9c046328a1e2b3ece4f222bb17e544ec9b))

## [0.46.0](https://github.com/bayang/jelu/compare/v0.45.0...v0.46.0) (2023-12-04)


### Features

* allow image upload from server ([361bca6](https://github.com/bayang/jelu/commit/361bca61e3f29270963cd2d16c42a7c494f6647a))


### Bug Fixes

* remove old bulma css ([277eaf9](https://github.com/bayang/jelu/commit/277eaf959eb99cecaa183c9ba473d83c12b52de5))

## [0.45.0](https://github.com/bayang/jelu/compare/v0.44.0...v0.45.0) (2023-11-24)


### Features

* import metadata from files ([ee36d57](https://github.com/bayang/jelu/commit/ee36d57e5b9742c3e472d31ba6f96477048417fe))


### Bug Fixes

* test ([1408ba9](https://github.com/bayang/jelu/commit/1408ba941cb4cdd15adecc5f281693230658dbef))

## [0.44.0](https://github.com/bayang/jelu/compare/v0.43.0...v0.44.0) (2023-11-16)


### Features

* technical upgrades ([6e9b768](https://github.com/bayang/jelu/commit/6e9b7683be9b4a3415e4df864472aa74bee33b31))


### Bug Fixes

* ci ([8685a47](https://github.com/bayang/jelu/commit/8685a471eb4fa3a5aca3b3189aab89e3b275648f))
* ci upgrade ([23741c7](https://github.com/bayang/jelu/commit/23741c759f65af7d00369a5c0df00a342658f11d))

## [0.43.0](https://github.com/bayang/jelu/compare/v0.42.0...v0.43.0) (2023-11-12)


### Features

* allow tags management page ([a5e4af0](https://github.com/bayang/jelu/commit/a5e4af00ffd7c506e28eab2f2ffce958816cfac0))


### Bug Fixes

* bugfix ([aee0f33](https://github.com/bayang/jelu/commit/aee0f335b1d4ff7c2a45a64f7f8f71b293d0d6ca))
* delete label ([722f749](https://github.com/bayang/jelu/commit/722f749401a1ec8c763171371b939c616ed82899))

## [0.42.0](https://github.com/bayang/jelu/compare/v0.41.4...v0.42.0) (2023-11-01)


### Features

* allow multiple series per book ([a9c5979](https://github.com/bayang/jelu/commit/a9c5979e93c5bcb48b324fe942d9708d8de67021))

### [0.41.4](https://github.com/bayang/jelu/compare/v0.41.3...v0.41.4) (2023-09-01)

### [0.41.3](https://github.com/bayang/jelu/compare/v0.41.2...v0.41.3) (2023-08-28)


### Bug Fixes

* Crowdin updates ([932e205](https://github.com/bayang/jelu/commit/932e20531a7846e2124a74a92e53350fc401cc30))

### [0.41.2](https://github.com/bayang/jelu/compare/v0.41.1...v0.41.2) (2023-07-15)


### Bug Fixes

* crowdin updates ([181ac8d](https://github.com/bayang/jelu/commit/181ac8d0903708634c10ee903324c7850d8f9939))

### [0.41.1](https://github.com/bayang/jelu/compare/v0.41.0...v0.41.1) (2023-07-15)


### Bug Fixes

* add link to search docs ([1545fe3](https://github.com/bayang/jelu/commit/1545fe3154a21b9ab804682c8158c8d8f54b6ca3))

## [0.41.0](https://github.com/bayang/jelu/compare/v0.40.0...v0.41.0) (2023-07-15)


### Features

* add full text search ([ec9f714](https://github.com/bayang/jelu/commit/ec9f714cab15ea80426c9b7ca556e9ac03537a27))


### Bug Fixes

* ktlint upgrade ([5430a7e](https://github.com/bayang/jelu/commit/5430a7e84552ab23dbf59bec368c93ae81566f23))

## [0.40.0](https://github.com/bayang/jelu/compare/v0.39.0...v0.40.0) (2023-07-07)


### Features

* protected ldap login ([7e59159](https://github.com/bayang/jelu/commit/7e5915998b66bcc6cbcbb7565ccb7d5ad2fcd45c))

## [0.39.0](https://github.com/bayang/jelu/compare/v0.38.1...v0.39.0) (2023-06-07)


### Features

* see other users full to-read lists and book lists ([ec71709](https://github.com/bayang/jelu/commit/ec71709d965b62dcd91685921b18ba9916cd31ca))

### [0.38.1](https://github.com/bayang/jelu/compare/v0.38.0...v0.38.1) (2023-05-25)


### Bug Fixes

* linting ([763e200](https://github.com/bayang/jelu/commit/763e2008920c401456b11b047e444535539c02d0))
* linting ([b48372b](https://github.com/bayang/jelu/commit/b48372b301512120469fd96b85974f6d89997a56))
* linting ([f66d5ce](https://github.com/bayang/jelu/commit/f66d5ce8b6fa609061c8660cc807505f64c0d891))
* linting ([311b5cc](https://github.com/bayang/jelu/commit/311b5cc4bc432907e3c67326aae88c1a6a04cfa7))
* try to fix synology [#67](https://github.com/bayang/jelu/issues/67) ([0b3ab86](https://github.com/bayang/jelu/commit/0b3ab8616b315ca5d17fdcaa3c768a9e965f6bb6))

## [0.38.0](https://github.com/bayang/jelu/compare/v0.37.0...v0.38.0) (2023-03-25)


### Features

* plugins refactor ([bd942fe](https://github.com/bayang/jelu/commit/bd942fe20fd202d4639889e80c57a39ab3597eb6))


### Bug Fixes

* make linter happy ([5fd65ee](https://github.com/bayang/jelu/commit/5fd65eec5f7353ca76b6914131ea6a98ce0956ce))

## [0.37.0](https://github.com/bayang/jelu/compare/v0.36.0...v0.37.0) (2023-02-16)


### Features

* add latest reviews from current instance on home page ([258dcb3](https://github.com/bayang/jelu/commit/258dcb338bbbc84c0bb424f001d2b694aae197a6))
* add possibility to see others [#61](https://github.com/bayang/jelu/issues/61) ([b2014d6](https://github.com/bayang/jelu/commit/b2014d6aed7267dca5f382394e303b8c212cf77f))


### Bug Fixes

* add translators field on add book page ([863342a](https://github.com/bayang/jelu/commit/863342a80f85a6ecb3c7cff54f2379aeeb64090e))

## [0.36.0](https://github.com/bayang/jelu/compare/v0.35.2...v0.36.0) (2023-01-19)


### Features

* add possibility to import metadata on existing book ([aefed8a](https://github.com/bayang/jelu/commit/aefed8a1b34ed5b5fdb0e5cae40de1839db5b73e))

### [0.35.2](https://github.com/bayang/jelu/compare/v0.35.1...v0.35.2) (2023-01-08)


### Bug Fixes

* imports should not overwrite existing images [#58](https://github.com/bayang/jelu/issues/58) ([21f96d7](https://github.com/bayang/jelu/commit/21f96d729b46285f9dcf018453ee7841c3724735))
* jelu theme readability bug ([833e887](https://github.com/bayang/jelu/commit/833e8873c902058f65e7ebade9569b60cf25924c))

### [0.35.1](https://github.com/bayang/jelu/compare/v0.35.0...v0.35.1) (2023-01-07)


### Bug Fixes

* jelu accent color ([125cfd4](https://github.com/bayang/jelu/commit/125cfd4e8cfaaac49a7f7b4fc308b7024b8ec9f8))

## [0.35.0](https://github.com/bayang/jelu/compare/v0.34.4...v0.35.0) (2023-01-03)


### Features

* add page count to user stats [#55](https://github.com/bayang/jelu/issues/55) ([f51df40](https://github.com/bayang/jelu/commit/f51df409fce94a7c1f66e47864ee58f3c2372166))
* allow turning off cover fetching on isbn list import [#57](https://github.com/bayang/jelu/issues/57) ([8348544](https://github.com/bayang/jelu/commit/834854438775e3f87accfc0873774ec43602d969))

### [0.34.4](https://github.com/bayang/jelu/compare/v0.34.3...v0.34.4) (2022-12-18)


### Bug Fixes

* errors caused by upgrades ([25fe2cf](https://github.com/bayang/jelu/commit/25fe2cf8998c5a5e21af4880ec63c2b36cf8adbe))
* goodreads csv parser ([33f0607](https://github.com/bayang/jelu/commit/33f06077580e3f46247933eeaf9d7899bf7e7b03))

### [0.34.3](https://github.com/bayang/jelu/compare/v0.34.2...v0.34.3) (2022-12-08)


### Bug Fixes

* modification date not updated on userbook ([98ca115](https://github.com/bayang/jelu/commit/98ca115573c8163c6cf264b6000832a8f51f7341))
* wrong sort in history view [#51](https://github.com/bayang/jelu/issues/51) ([a6cfa5b](https://github.com/bayang/jelu/commit/a6cfa5ba606583c675350a4fd4fe543ac4a0f0f0))

### [0.34.2](https://github.com/bayang/jelu/compare/v0.34.1...v0.34.2) (2022-11-14)


### Bug Fixes

* New translations ([81baced](https://github.com/bayang/jelu/commit/81bacede6016bec800bacd14994866e1784ed307))

### [0.34.1](https://github.com/bayang/jelu/compare/v0.34.0...v0.34.1) (2022-11-14)


### Bug Fixes

* improve date pickers usablility for events ([668593c](https://github.com/bayang/jelu/commit/668593c75d5cfe1f3ac0d89b3b2076a943d17545))

## [0.34.0](https://github.com/bayang/jelu/compare/v0.33.1...v0.34.0) (2022-11-07)


### Features

* add barcode scanning ([63492ca](https://github.com/bayang/jelu/commit/63492ca9a8a65549d8d3dea6fa999f7c48719df3))

### [0.33.1](https://github.com/bayang/jelu/compare/v0.33.0...v0.33.1) (2022-10-24)


### Bug Fixes

* date handling in history view after events modification ([1d2edd9](https://github.com/bayang/jelu/commit/1d2edd9299c17f3036fb7529ebce6c18e0d9e8ea))
* impossible to edit an event in some conditions [#49](https://github.com/bayang/jelu/issues/49) ([cfa0ad1](https://github.com/bayang/jelu/commit/cfa0ad1f3b497d31944392dd40f113b4e879f3b9))

## [0.33.0](https://github.com/bayang/jelu/compare/v0.32.0...v0.33.0) (2022-10-23)


### Features

* add start date and end date on events for better clarity [#39](https://github.com/bayang/jelu/issues/39) ([1bbbea4](https://github.com/bayang/jelu/commit/1bbbea4118e2fc549d615b759d0fa27a5ab127af))


### Bug Fixes

* logging level in a method ([e6d32a1](https://github.com/bayang/jelu/commit/e6d32a1cc5ac085ca2a029872bdf2c859ca1cfc2))
* redirect loop on history page ([43e40ca](https://github.com/bayang/jelu/commit/43e40ca3dc7cf5b0755c80b5056ba037d7285d3e))
* reviews too narrow ([7090304](https://github.com/bayang/jelu/commit/7090304c40e0ac925c06da760ccd0c968dcbfeb8))

## [0.32.0](https://github.com/bayang/jelu/compare/v0.31.0...v0.32.0) (2022-10-02)


### Features

* add translators on books [#27](https://github.com/bayang/jelu/issues/27) ([70e2840](https://github.com/bayang/jelu/commit/70e2840671d5e91be22505569b4a2c4b84852d6c))
* mark book as borrowed [#46](https://github.com/bayang/jelu/issues/46) ([950c6c7](https://github.com/bayang/jelu/commit/950c6c79d09696cdd78e2e793c61cbe81b3ea4ac))

## [0.31.0](https://github.com/bayang/jelu/compare/v0.30.0...v0.31.0) (2022-09-20)


### Features

* add google metadata provider [#36](https://github.com/bayang/jelu/issues/36) [#37](https://github.com/bayang/jelu/issues/37) ([cb14ee8](https://github.com/bayang/jelu/commit/cb14ee8610cce87acd4e6bc433cf2e4a1192747a))


### Bug Fixes

* infinite redirect loop [#44](https://github.com/bayang/jelu/issues/44) ([84ce9fb](https://github.com/bayang/jelu/commit/84ce9fb9374345c7ee503c16e049e1dbe8ef310a))
* regression in UI after modifying metadata image field ([18a9281](https://github.com/bayang/jelu/commit/18a928191dfce82da3e97210524a3edbb7ef7d61))

## [0.30.0](https://github.com/bayang/jelu/compare/v0.29.0...v0.30.0) (2022-09-13)


### Features

* use markdown to write reviews [#40](https://github.com/bayang/jelu/issues/40) ([0e11bbb](https://github.com/bayang/jelu/commit/0e11bbb5d1c03f8448e44db83d1742c07918036d))


### Bug Fixes

* increase password length in login input [#43](https://github.com/bayang/jelu/issues/43) ([7568cc9](https://github.com/bayang/jelu/commit/7568cc99f3741b48d80a468a51926f8172692e46))
* prevent temporary cover images conflicts in caches [#41](https://github.com/bayang/jelu/issues/41) ([b1482e1](https://github.com/bayang/jelu/commit/b1482e1b3d9f73c4213123614a79c3f0c06a1f32))


## [0.29.0](https://github.com/bayang/jelu/compare/v0.28.0...v0.29.0) (2022-08-20)


### Features

* add proxy authentication ([a2f2554](https://github.com/bayang/jelu/commit/a2f2554eae89bf5e240c15d259bf0cd7b7766b03))
* also export dropped dates and currently reading [#24](https://github.com/bayang/jelu/issues/24) ([d81afbe](https://github.com/bayang/jelu/commit/d81afbe295fdaeb8e9d40812f7662d2606a42426))


### Bug Fixes

* remove useless line in dockerfile ([2277c94](https://github.com/bayang/jelu/commit/2277c9486f73866433914995778c9af8164e0625))

## [0.28.0](https://github.com/bayang/jelu/compare/v0.27.0...v0.28.0) (2022-08-19)


### Features

* import books from ISBN list ([0bf1c49](https://github.com/bayang/jelu/commit/0bf1c498bb5179bbc21d3f276cc213dbaa3b816b))

## [0.27.0](https://github.com/bayang/jelu/compare/v0.26.3...v0.27.0) (2022-08-11)


### Features

* add series page [#20](https://github.com/bayang/jelu/issues/20) ([d55010a](https://github.com/bayang/jelu/commit/d55010a038ce22492cd43f252625cb55d482a32d))


### Bug Fixes

* breakpoints management [#23](https://github.com/bayang/jelu/issues/23) ([24316cc](https://github.com/bayang/jelu/commit/24316cc03b5d5c5252e1b4a2f122f37b3653c8ed))

### [0.26.3](https://github.com/bayang/jelu/compare/v0.26.2...v0.26.3) (2022-08-07)


### Bug Fixes

* changing YAML order for a working  file. ([#26](https://github.com/bayang/jelu/issues/26)) ([cbd67d7](https://github.com/bayang/jelu/commit/cbd67d7b27cbdb1770cfbbb9c7de85d60aefd9b1))

### [0.26.2](https://github.com/bayang/jelu/compare/v0.26.1...v0.26.2) (2022-07-27)


### Bug Fixes

* add missing translations ([35d2c82](https://github.com/bayang/jelu/commit/35d2c828b83e54f85e81ffc780b25b0acaffc0b3))

### [0.26.1](https://github.com/bayang/jelu/compare/v0.26.0...v0.26.1) (2022-07-26)


### Bug Fixes

* update readme for translations ([5c5b984](https://github.com/bayang/jelu/commit/5c5b984710ba396d2bfe4731644895cac211802c))

## [0.26.0](https://github.com/bayang/jelu/compare/v0.25.1...v0.26.0) (2022-07-26)


### Features

* add possibility to write reviews ([b9611d1](https://github.com/bayang/jelu/commit/b9611d13fc9449e733add6beb3f996dc7f3281a5))


### Bug Fixes

* dockerfile build for amd64 ([a7599e8](https://github.com/bayang/jelu/commit/a7599e804631554685ac28191b2e429491d409f1))

### [0.25.1](https://github.com/bayang/jelu/compare/v0.25.0...v0.25.1) (2022-07-04)


### Bug Fixes

* add missing translations ([132ccd1](https://github.com/bayang/jelu/commit/132ccd1d2a35d7461e1a373b3078043e0e178a86))

## [0.25.0](https://github.com/bayang/jelu/compare/v0.24.2...v0.25.0) (2022-07-04)


### Features

* add bulk edition ([893683b](https://github.com/bayang/jelu/commit/893683b1ffedd8a0a36eb8c8353a7dcd150e386c))


### Bug Fixes

* not being able to modify a cover [#17](https://github.com/bayang/jelu/issues/17) ([2fa6428](https://github.com/bayang/jelu/commit/2fa6428aa9f363792673f078ec34f3beda051006))
* some bad requests could be sent on tag page ([3009f8b](https://github.com/bayang/jelu/commit/3009f8b1018000a3d84e08b29d3fe82d40d94a28))

### [0.24.2](https://github.com/bayang/jelu/compare/v0.24.1...v0.24.2) (2022-06-16)


### Bug Fixes

* german translations ([0b1d64e](https://github.com/bayang/jelu/commit/0b1d64eeb14210ae48f3e7aef5b29f8cc16c511a))

### [0.24.1](https://github.com/bayang/jelu/compare/v0.24.0...v0.24.1) (2022-06-16)


### Bug Fixes

* Crowdin update ([460660b](https://github.com/bayang/jelu/commit/460660b41c0d8ee3ad92f59d5ac50cd121e029b1))

## [0.24.0](https://github.com/bayang/jelu/compare/v0.23.1...v0.24.0) (2022-06-16)


### Features

* add custom shelves ([3b50e2f](https://github.com/bayang/jelu/commit/3b50e2f3ad60fcefe1f8ab4e18f4d23833090663))


### Bug Fixes

* test failing on ci ([f5e92d7](https://github.com/bayang/jelu/commit/f5e92d7b3735d55d80e48706b867328b36395d8d))
* textarea size on add book form ([98ace19](https://github.com/bayang/jelu/commit/98ace19da8d80f9caf2e27de2eebec05781aa6d2))

### [0.23.1](https://github.com/bayang/jelu/compare/v0.23.0...v0.23.1) (2022-06-10)

## [0.23.0](https://github.com/bayang/jelu/compare/v0.22.2...v0.23.0) (2022-06-10)


### Features

* add possibility to filter by owned field ([7afde5c](https://github.com/bayang/jelu/commit/7afde5cc594ab45e85d69325222e589023025e45))


### Bug Fixes

* prevent exception that could happen when adding a book ([231997b](https://github.com/bayang/jelu/commit/231997b8e1583654aef521222b4794a5c628807f))
* provide feedback that initial user data is loading ([9c21f71](https://github.com/bayang/jelu/commit/9c21f71ecede0b21732d2da412b02c09d550ee39))
* remove wrong locale files ([e89a69a](https://github.com/bayang/jelu/commit/e89a69a1237273fff28bf5d62cfb375a7a17fa2d))

### [0.22.2](https://github.com/bayang/jelu/compare/v0.22.1...v0.22.2) (2022-06-03)


### Bug Fixes

* czech translation ([9767cc6](https://github.com/bayang/jelu/commit/9767cc656b33971656373d302e78ce1aba5c0195))

### [0.22.1](https://github.com/bayang/jelu/compare/v0.22.0...v0.22.1) (2022-06-02)


### Bug Fixes

* cannot remove cover ([dd77866](https://github.com/bayang/jelu/commit/dd7786655c0f014910530bcb08dfa819c8c2efdd))
* dev config ([a1df750](https://github.com/bayang/jelu/commit/a1df750b4350864d14182dd2b7fdad33a6aa02a3))
* do not display create initial user with ldap enabled ([bb0a89c](https://github.com/bayang/jelu/commit/bb0a89cd2ac15d0a2156d866b7d9dead0da75106))

## [0.22.0](https://github.com/bayang/jelu/compare/v0.21.0...v0.22.0) (2022-06-02)


### Features

* ldap support ([9def6a1](https://github.com/bayang/jelu/commit/9def6a1ca3dbccb2b641a8027f5bee30ec2fa691))


### Bug Fixes

* please linter ([284f81c](https://github.com/bayang/jelu/commit/284f81cb500981013b302f60744f7f2a4cf8ffae))

## [0.21.0](https://github.com/bayang/jelu/compare/v0.20.0...v0.21.0) (2022-05-25)


### Features

* add history view ([e8ffb83](https://github.com/bayang/jelu/commit/e8ffb83520bbf1b77aed8c8089c85bf1f0fad5fe))


### Bug Fixes

* event modal too small ([6215202](https://github.com/bayang/jelu/commit/62152021935f6529a8cf287d024856663b120c4b))
* remove to-read flag when book is finished ([c88a4fe](https://github.com/bayang/jelu/commit/c88a4fedfe3314de0ffaf16300ad92880bc3418e))

## [0.20.0](https://github.com/bayang/jelu/compare/v0.19.0...v0.20.0) (2022-05-20)


### Features

* put the search bar in nav bar ([2fef551](https://github.com/bayang/jelu/commit/2fef5515b912abc08ebe2b3481065e42deb00d00))


### Bug Fixes

* add loading indicator when loading stats ([ffe2e35](https://github.com/bayang/jelu/commit/ffe2e35b6e67eb3ac18a7f7402f96fba42900486))
* stats order ([83199f7](https://github.com/bayang/jelu/commit/83199f7d743db8fceaa813f70cbaa86104894ed2))
* try to make metadata grabber work on ARM ([8cea20f](https://github.com/bayang/jelu/commit/8cea20fb770926677f03ef8aa4933edf3a8f2e6c))

## [0.19.0](https://github.com/bayang/jelu/compare/v0.18.0...v0.19.0) (2022-05-15)


### Features

* add stats ([8b7368b](https://github.com/bayang/jelu/commit/8b7368b414821369fb1508866009e06af36d8ff1))

## [0.18.0](https://github.com/bayang/jelu/compare/v0.17.1...v0.18.0) (2022-05-13)


### Features

* add csv export ([efc8d9f](https://github.com/bayang/jelu/commit/efc8d9fc6eb4796bda0e264987daea3e69d130fc))


### Bug Fixes

* linting ([6497ebf](https://github.com/bayang/jelu/commit/6497ebf140a748b4dea7e744fa0d41bd8cb86ff8))
* on import do not try to fetch metadata if binary not configured [#3](https://github.com/bayang/jelu/issues/3) ([92f2b90](https://github.com/bayang/jelu/commit/92f2b90f755f79254737eec394232066d5eb73a0))

### [0.17.1](https://github.com/bayang/jelu/compare/v0.17.0...v0.17.1) (2022-05-03)


### Bug Fixes

* csv import tests and bugfixes ([3b60f9b](https://github.com/bayang/jelu/commit/3b60f9b49c0a557f6038e0a82ea5d108175ccac6))
* prompt styling ([1778628](https://github.com/bayang/jelu/commit/1778628edf25eef9791096ef791786abbc41d01a))

## [0.17.0](https://github.com/bayang/jelu/compare/v0.16.1...v0.17.0) (2022-05-01)


### Features

* allow more customization of cors ([19e0a58](https://github.com/bayang/jelu/commit/19e0a5885f9804e8667dbab1472e4df6d24102bd))

### [0.16.1](https://github.com/bayang/jelu/compare/v0.16.0...v0.16.1) (2022-04-24)


### Bug Fixes

* readme markdown was corrupted ([798351c](https://github.com/bayang/jelu/commit/798351cd7d860f98e450346654c7999da70188cc))

## [0.16.0](https://github.com/bayang/jelu/compare/v0.15.0...v0.16.0) (2022-04-24)


### Features

* provide embed code ([f8cde36](https://github.com/bayang/jelu/commit/f8cde3629c75211d8796c51f09bad5c6a6b03435))

## [0.15.0](https://github.com/bayang/jelu/compare/v0.14.0...v0.15.0) (2022-04-19)


### Features

* add ability to create users ([631e386](https://github.com/bayang/jelu/commit/631e3865f6623d1b73aa86e67f260c65fd4feb3a))
* add shortcuts explanation ([6f3f05f](https://github.com/bayang/jelu/commit/6f3f05f34a3f2502e10cbbf30cbc5216c747030e))
* add user editing ([c6a0700](https://github.com/bayang/jelu/commit/c6a07008ee86dc11043a66660192d4749d9c5fbc))
* refactor shortcuts explanation ([ac316c9](https://github.com/bayang/jelu/commit/ac316c966a9843793c6bd8a0683f8b35fb06b847))


### Bug Fixes

* add missing translation ([51ea3cf](https://github.com/bayang/jelu/commit/51ea3cfd82c4850e89753767ad2c187adf89fc66))
* book update image bug ([0365533](https://github.com/bayang/jelu/commit/0365533f37555d14e88a9d145253f659b5135985))
* inconsistent font size in filter bar ([d0f905c](https://github.com/bayang/jelu/commit/d0f905c06100de85b3f666042996084965bb9286))
* remove useless variable ([c5cbe6c](https://github.com/bayang/jelu/commit/c5cbe6cd89f2fd0f739c1e317b9093fc56db1312))
* runtime warnings ([2515662](https://github.com/bayang/jelu/commit/2515662d8078684a9c2b7faea9fe33295b62b57b))
* some modal had wrong config ([543f09f](https://github.com/bayang/jelu/commit/543f09fc4fbc06890ed9f6a3a97ba962ec6167e6))

## [0.14.0](https://github.com/bayang/jelu/compare/v0.13.1...v0.14.0) (2022-04-12)


### Features

* add i18n, english and french for now ([4f5ee22](https://github.com/bayang/jelu/commit/4f5ee224fb573dd5caf4fdc364c551a3f84ace69))


### Bug Fixes

* fix hp sometimes not updated ([e131306](https://github.com/bayang/jelu/commit/e13130601cb329b867072b651606aa35518d6b05))

### [0.13.1](https://github.com/bayang/jelu/compare/v0.13.0...v0.13.1) (2022-04-07)


### Bug Fixes

* add loading on book detail ([e580d25](https://github.com/bayang/jelu/commit/e580d258723a894aa4d17c61d8450f6227976138))
* delete popup positioning ([ae454f9](https://github.com/bayang/jelu/commit/ae454f93444203ca975fe7e06bfd66f68b231c4d))
* missing gap between cards on author page ([2be56b2](https://github.com/bayang/jelu/commit/2be56b2da425b75f4d6d3de581737e80a6783585))
* missing space between date and notes on add book page ([18c862f](https://github.com/bayang/jelu/commit/18c862ff76dead154794f424a1d06edd127f7cff))

## [0.13.0](https://github.com/bayang/jelu/compare/v0.12.0...v0.13.0) (2022-04-04)


### Features

* add jelu theme ([318d161](https://github.com/bayang/jelu/commit/318d161a1955fb2567613adf9232cc8fbdee0f03))
* search ux improvement ([7117cfc](https://github.com/bayang/jelu/commit/7117cfc3ccb386091ed94a9d1783d7c22f09e1fb))


### Bug Fixes

* class clean up ([9ecaff2](https://github.com/bayang/jelu/commit/9ecaff2c04ee8adceb5abe87c8377653a5228cce))
* tag aspect on book detail ([9de6fab](https://github.com/bayang/jelu/commit/9de6fab920c39c9c40b455765f596e740699ae5c))

## [0.12.0](https://github.com/bayang/jelu/compare/v0.11.0...v0.12.0) (2022-04-02)


### Features

* more loading feedback ([7ad0b25](https://github.com/bayang/jelu/commit/7ad0b25c9861a7a331666085734d18db899cf5a8))


### Bug Fixes

* remove useless class ([1729b6a](https://github.com/bayang/jelu/commit/1729b6a8b11f022346f8d2ed56c592e0f755fc5c))
* small improvements in UI ([7b27e94](https://github.com/bayang/jelu/commit/7b27e94ae9228c8291180c61e46b37d292674312))

## [0.11.0](https://github.com/bayang/jelu/compare/v0.10.0...v0.11.0) (2022-04-01)


### Features

* possibility to merge authors ([6e237f1](https://github.com/bayang/jelu/commit/6e237f18b677b2ea1b12af24d31891d14519dc99))


### Bug Fixes

* add indeterminate state while loading ([96fc1e5](https://github.com/bayang/jelu/commit/96fc1e543787129cff9bfe46ff703e95fb0dd889))
* add progress indicators for login and metadata search ([f22a91f](https://github.com/bayang/jelu/commit/f22a91fe47b22ca84b6eabdbe22a592b9470cb14))
* author picture sometimes absent from metadata search ([b7e0d16](https://github.com/bayang/jelu/commit/b7e0d161a6602cf5a06a95ad83e3eecfb82aa680))
* error while resizing image ([d43c054](https://github.com/bayang/jelu/commit/d43c054f36282bbcbbabe390caf162d3ab772b88))
* progress positioning on book add ([e8227dd](https://github.com/bayang/jelu/commit/e8227ddaa670947f843223bd29b943cfa5f6d92d))
* update book requests ([f0b95e8](https://github.com/bayang/jelu/commit/f0b95e8e91f8f55f7fd6d93e0971321d500e7dd3))

## [0.10.0](https://github.com/bayang/jelu/compare/v0.9.0...v0.10.0) (2022-03-14)


### Features

* author page improvement ([129ae35](https://github.com/bayang/jelu/commit/129ae35f87438da74140d2acf43f8497f4b91490))
* show progress on events modal ([d4605fe](https://github.com/bayang/jelu/commit/d4605fe9c8ca0503126d8c0d6cc7787e2c2b60cc))


### Bug Fixes

* metadata source deserialization ([bbf1c2a](https://github.com/bayang/jelu/commit/bbf1c2adedaf5584c31608302c9f38792d1ededd))

## [0.9.0](https://github.com/bayang/jelu/compare/v0.8.0...v0.9.0) (2022-03-13)


### Features

* author page ([1e80993](https://github.com/bayang/jelu/commit/1e80993b1d99047b4f779ab23eea79fd7b0ffd7c))


### Bug Fixes

* ui error ([90db9c7](https://github.com/bayang/jelu/commit/90db9c7f97d17ffb4e7d656c9e32905ad70eb894))

## [0.8.0](https://github.com/bayang/jelu/compare/v0.7.0...v0.8.0) (2022-03-08)


### Features

* dynamically provide version at runtime ([3dce43e](https://github.com/bayang/jelu/commit/3dce43eb0caaa0a89de45aa80c424ce13d187af0))


### Bug Fixes

* forward controller pattern ([c528b51](https://github.com/bayang/jelu/commit/c528b51b7b76964ddfc84c4b8e1d8c4f61046d51))
* forwarding pages broken after boot 2.6 update ([323d644](https://github.com/bayang/jelu/commit/323d6441e0292619c6dfed5835e8e3feb2fc3d9f))
* removing authors when updating was not possible ([dba5bd3](https://github.com/bayang/jelu/commit/dba5bd31492b0113775bbfe77f0a7fa6d193e292))

## [0.7.0](https://github.com/bayang/jelu/compare/v0.6.0...v0.7.0) (2022-03-04)


### Features

* add index on book image for future use ([39c9c2a](https://github.com/bayang/jelu/commit/39c9c2a4ae4f7d504bdd123dee6596b429889124))
* add REST documentation ([aa3c635](https://github.com/bayang/jelu/commit/aa3c635cc2ea0f7a5a01ca9006263482fcc479b7))
* resize covers and discarded files management ([072cd16](https://github.com/bayang/jelu/commit/072cd166ea81bba8b4e5f93d14ae8655d24831c2))


### Bug Fixes

* wrong icon on tag add ([fc75358](https://github.com/bayang/jelu/commit/fc753582b23884f2c9f6dc29c481464b9d6e8808))

## [0.6.0](https://github.com/bayang/jelu/compare/v0.5.0...v0.6.0) (2022-02-27)


### Features

* add readme and documentation ([e3904be](https://github.com/bayang/jelu/commit/e3904be974ede715644f639942e2c2d4a13a0da1))


### Bug Fixes

* only propose existing import sources ([ca01328](https://github.com/bayang/jelu/commit/ca013287d4088c5a8cdc1b989f3550eac4f7ba4c))

## [0.5.0](https://github.com/bayang/jelu/compare/v0.4.0...v0.5.0) (2022-02-26)


### Features

* add date added sort on books list ([ee81a1a](https://github.com/bayang/jelu/commit/ee81a1ac9c6d849dcf726245a8c607f1554470b9))
* choose event date on import ([0a4ac5d](https://github.com/bayang/jelu/commit/0a4ac5dd1d4eeec3fc26500e488369622932f3de))
* search by authors and tags ([7be74ff](https://github.com/bayang/jelu/commit/7be74ffb28585b8e21cd6f02dde2ed5f50901075))


### Bug Fixes

* duplicate events while editing ([dbe3ad1](https://github.com/bayang/jelu/commit/dbe3ad113419e388686ab0f72d2b016af47a3cb9))
* duplicate tags management on update ([b153036](https://github.com/bayang/jelu/commit/b1530363a797226d8af5a511a4422b8348b106ca))
* keyboard shortcuts navigation ([0410ba0](https://github.com/bayang/jelu/commit/0410ba0b627c0fbb90c10b67bf636fb09b48d9db))
* redirect after import ([5219625](https://github.com/bayang/jelu/commit/5219625285798719b09204e70f246d2dd984e5b5))
* show book title in tooltip on card ([a446354](https://github.com/bayang/jelu/commit/a44635459400ce06bf5fa93f8c8a8587d6875d51))
* submit auto import via enter ([e059a9f](https://github.com/bayang/jelu/commit/e059a9fd1ddc06d0307bcf1a106d350269995793))
* unstyled auto import buttons ([4fc1cfd](https://github.com/bayang/jelu/commit/4fc1cfddc37cc461325c7e7b0e15bd85eea222a8))

## [0.4.0](https://github.com/bayang/jelu/compare/v0.3.0...v0.4.0) (2022-02-21)


### Features

* reading events management ([448e284](https://github.com/bayang/jelu/commit/448e284d644dd460373fb74fe0650cbac72c71e2))


### Bug Fixes

* bug in some queries ([af546dd](https://github.com/bayang/jelu/commit/af546dde932fed3e5f3a1e95615ef60cd76b9d70))

## [0.3.0](https://github.com/bayang/jelu/compare/v0.2.0...v0.3.0) (2022-02-14)


### Features

* add sorting and filtering ([f2f1793](https://github.com/bayang/jelu/commit/f2f1793707425c3af0f59679353dffc034810954))
* change banner ([da1012f](https://github.com/bayang/jelu/commit/da1012fdb1530d778f058611142362dfc57d349d))


### Bug Fixes

* pagination propagation in query ([094f0c5](https://github.com/bayang/jelu/commit/094f0c54f4ecea03da1938c73ba441318ff3adbf))
* quotes width ([45dd9b0](https://github.com/bayang/jelu/commit/45dd9b0cedabb597cdbd52b9fe78086071e9a3e0))

## [0.2.0](https://github.com/bayang/jelu/compare/v0.1.8...v0.2.0) (2022-01-27)


### Features

* csv import ([4515285](https://github.com/bayang/jelu/commit/4515285b93f6151281c9e9137e7cf1ef56842ed5))


### Bug Fixes

* check server capabilities before calling them ([c0cec92](https://github.com/bayang/jelu/commit/c0cec9234c91e8d8c6969045f080efd0098cca5e))
* ellipsis on book card ([0197911](https://github.com/bayang/jelu/commit/019791127addfb840fb284f5c1b715c4897da2cd))

### [0.1.8](https://github.com/bayang/jelu/compare/v0.1.7...v0.1.8) (2022-01-18)


### Bug Fixes

* amd calibre folder ([e611dbb](https://github.com/bayang/jelu/commit/e611dbbafc1ade566123588af50581f2c205fdb9))
* revert add amd calibre folder" ([ec29273](https://github.com/bayang/jelu/commit/ec29273fd2315c5526c6663533b1bc2bbcf2a26c))

### [0.1.7](https://github.com/bayang/jelu/compare/v0.1.6...v0.1.7) (2022-01-18)


### Bug Fixes

* arm images push ([02c6ee0](https://github.com/bayang/jelu/commit/02c6ee0194f971bd7924dee57dd9961a064db4fb))

### [0.1.6](https://github.com/bayang/jelu/compare/v0.1.5...v0.1.6) (2022-01-18)


### Bug Fixes

* arm images push ([d4dd73b](https://github.com/bayang/jelu/commit/d4dd73b8085be730329c38b9c90d8baf129fa647))

### [0.1.5](https://github.com/bayang/jelu/compare/v0.1.4...v0.1.5) (2022-01-18)


### Bug Fixes

* arm images push ([24d4df6](https://github.com/bayang/jelu/commit/24d4df674a02de819f02a8092a027f70ee7563c0))

### [0.1.4](https://github.com/bayang/jelu/compare/v0.1.3...v0.1.4) (2022-01-18)


### Bug Fixes

* arm dockerfile ([2fdfce2](https://github.com/bayang/jelu/commit/2fdfce2c13dd102293aa617af6396c3175a7353d))
* arm images ([fbbed72](https://github.com/bayang/jelu/commit/fbbed7256374fca6907f434f8ac8598d873a9667))
* arm images file ([b2d528b](https://github.com/bayang/jelu/commit/b2d528b9ac9e95e663bc269fae1d3d356f22b3c6))
* metadata fetcher path ([6c8d452](https://github.com/bayang/jelu/commit/6c8d45282791d21e0476224df962fe3482209621))

### [0.1.3](https://github.com/bayang/jelu/compare/v0.1.2...v0.1.3) (2022-01-16)


### Bug Fixes

* frontend config ([39952b7](https://github.com/bayang/jelu/commit/39952b73fd6ffacca623e3e64c7b5a529c1da1ce))
* frontend url ([dfda488](https://github.com/bayang/jelu/commit/dfda488e7a2df09bc832257323abd07d6ef45e7b))
* linting ([246d2ae](https://github.com/bayang/jelu/commit/246d2ae402e248cf131c06d15e701d98a993865a))

### [0.1.2](https://github.com/bayang/jelu/compare/v0.1.1...v0.1.2) (2022-01-16)


### Bug Fixes

* ci replacement ([0dabd70](https://github.com/bayang/jelu/commit/0dabd7020c062c692310fc4476d302af51e11edc))

### [0.1.1](https://github.com/bayang/jelu/compare/v0.1.0...v0.1.1) (2022-01-16)


### Bug Fixes

* ci ([2e82a0b](https://github.com/bayang/jelu/commit/2e82a0b6618e8b0c6fbeb35c4dd715e27794d5f6))
* ci ([61aec3e](https://github.com/bayang/jelu/commit/61aec3e742907464539a8aedee4224375d374a1d))
* ci ([f8eba27](https://github.com/bayang/jelu/commit/f8eba274d3bf968b34040453e7c1a4eef2d5a861))
* ci and build fix ([70388ed](https://github.com/bayang/jelu/commit/70388ed4b0ef0eff676c7f4191bf373e3d2dcb06))
* ci release ([c54be1d](https://github.com/bayang/jelu/commit/c54be1dd7fecc3a63d6945fa9cea6e3b6ed85ec4))
* ci release ([ec4da5f](https://github.com/bayang/jelu/commit/ec4da5f68e73c81f8f6be8eddc32e1720af10a53))
* ci release ([c4cb62f](https://github.com/bayang/jelu/commit/c4cb62f41d1a9492256ae89dfabed7ca1ca53980))
