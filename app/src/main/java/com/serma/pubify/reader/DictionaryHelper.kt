package com.serma.pubify.reader

data class WordDefinition(
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String,
    val synonyms: List<String> = emptyList()
)

object DictionaryHelper {

    // Comprehensive offline basic dictionary covering fundamental literary, narrative, and general vocabulary
    private val localDictionary: Map<String, WordDefinition> = mapOf(
        "abandon" to WordDefinition("Abandon", "/əˈbændən/", "verb", "To leave behind or give up completely; to cease to support or look after.", "They were forced to abandon their campsite as the storm approached.", listOf("desert", "forsake", "relinquish", "vacate")),
        "ability" to WordDefinition("Ability", "/əˈbɪləti/", "noun", "The possession of the means or skill to do something.", "Her unique ability to memorize entire chapters amazed everyone.", listOf("capability", "talent", "aptitude", "skill")),
        "abundant" to WordDefinition("Abundant", "/əˈbʌndənt/", "adjective", "Existing or available in large quantities; plentiful.", "The valley provided abundant fresh water and fruit.", listOf("plentiful", "copious", "rich", "bountiful")),
        "accept" to WordDefinition("Accept", "/əkˈsept/", "verb", "To consent to receive or undertake something offered.", "He decided to accept the challenge with an open mind.", listOf("receive", "embrace", "acknowledge", "welcome")),
        "achieve" to WordDefinition("Achieve", "/əˈtʃiːv/", "verb", "To successfully reach a desired goal, result, or effort.", "Through persistence she achieved what many thought impossible.", listOf("accomplish", "attain", "realize", "fulfill")),
        "acknowledge" to WordDefinition("Acknowledge", "/əkˈnɒlɪdʒ/", "verb", "To accept or admit the existence or truth of something.", "The author paused to acknowledge the influence of ancient folklore.", listOf("admit", "recognize", "concede", "grant")),
        "admire" to WordDefinition("Admire", "/ədˈmaɪə/", "verb", "To regard with respect, wonder, or warm approval.", "Readers admire the courageous spirit of the protagonist.", listOf("respect", "esteem", "revere", "appreciate")),
        "adventure" to WordDefinition("Adventure", "/ədˈventʃə/", "noun", "An unusual, exciting, and typically hazardous experience or undertaking.", "Setting sail at midnight began their greatest adventure.", listOf("journey", "exploit", "quest", "venture")),
        "affection" to WordDefinition("Affection", "/əˈfekʃn/", "noun", "A gentle feeling of fondness or liking.", "A look of deep affection passed between the old friends.", listOf("fondness", "warmth", "devotion", "tenderness")),
        "agony" to WordDefinition("Agony", "/ˈæɡəni/", "noun", "Extreme physical or mental suffering.", "The agonizing wait felt longer than the actual ordeal.", listOf("anguish", "torment", "pain", "suffering")),
        "allure" to WordDefinition("Allure", "/əˈlʊə/", "noun / verb", "The quality of being powerfully and mysteriously attractive or fascinating.", "The allure of uncharted islands pulled him back to the sea.", listOf("charm", "attraction", "fascination", "enticement")),
        "ambition" to WordDefinition("Ambition", "/æmˈbɪʃn/", "noun", "A strong desire to do or to achieve something, typically requiring determination.", "His quiet ambition was to write stories that outlived him.", listOf("aspiration", "goal", "drive", "purpose")),
        "ancient" to WordDefinition("Ancient", "/ˈeɪnʃənt/", "adjective", "Belonging to the very distant past and no longer in existence.", "An ancient oak stood sentinel beside the ruin.", listOf("antique", "archaic", "timeless", "primeval")),
        "anger" to WordDefinition("Anger", "/ˈæŋɡə/", "noun", "A strong feeling of annoyance, displeasure, or hostility.", "His sudden anger melted into deep sorrow.", listOf("rage", "fury", "indignation", "wrath")),
        "anguish" to WordDefinition("Anguish", "/ˈæŋɡwɪʃ/", "noun", "Severe mental or physical pain or distress.", "A cry of heartfelt anguish echoed through the silent woods.", listOf("agony", "torment", "grief", "heartache")),
        "anxiety" to WordDefinition("Anxiety", "/æŋˈzaɪəti/", "noun", "A feeling of worry, nervousness, or unease about an imminent event.", "She felt a lingering anxiety as the ship entered the fog.", listOf("worry", "apprehension", "dread", "unease")),
        "appear" to WordDefinition("Appear", "/əˈpɪə/", "verb", "To come into sight; to seem or give the impression of being.", "A faint light appeared on the eastern horizon.", listOf("emerge", "manifest", "seem", "materialize")),
        "appreciate" to WordDefinition("Appreciate", "/əˈpriːʃieɪt/", "verb", "To recognize the full worth of; to be grateful for.", "They learned to appreciate every quiet afternoon.", listOf("value", "cherish", "treasure", "prize")),
        "arduous" to WordDefinition("Arduous", "/ˈɑːdjuəs/", "adjective", "Involving or requiring strenuous effort; difficult and tiring.", "The climb along the cliffside was long and arduous.", listOf("onerous", "taxing", "laborious", "tough")),
        "aspire" to WordDefinition("Aspire", "/əˈspaɪə/", "verb", "To direct one's hopes or ambitions towards achieving something.", "He aspired to capture the majesty of the wilderness in words.", listOf("aim", "hope", "strive", "yearn")),
        "astonish" to WordDefinition("Astonish", "/əˈstɒnɪʃ/", "verb", "To surprise or impress someone greatly.", "The breathtaking vista never failed to astonish travelers.", listOf("amaze", "astound", "stun", "surprise")),
        "atmosphere" to WordDefinition("Atmosphere", "/ˈætməsfɪə/", "noun", "The pervading tone or mood of a place, situation, or work of art.", "The ancient library had a quiet, reverent atmosphere.", listOf("mood", "ambiance", "aura", "tone")),
        "awe" to WordDefinition("Awe", "/ɔː/", "noun", "A feeling of reverential respect mixed with fear or wonder.", "They stood in silent awe beneath the cathedral vault.", listOf("wonder", "reverence", "amazement", "veneration")),
        "beauty" to WordDefinition("Beauty", "/ˈbjuːti/", "noun", "A combination of qualities that pleases the aesthetic senses, especially sight.", "The timeless beauty of the sunset silenced the crowd.", listOf("loveliness", "elegance", "grace", "splendor")),
        "belief" to WordDefinition("Belief", "/bɪˈliːf/", "noun", "An acceptance that a statement is true or that something exists; trust.", "His unwavering belief gave strength to the weary party.", listOf("faith", "conviction", "trust", "confidence")),
        "betray" to WordDefinition("Betray", "/bɪˈtreɪ/", "verb", "To expose one's country or a person to an enemy by treachery.", "She would never betray the confidence of her closest ally.", listOf("deceive", "break faith", "double-cross", "expose")),
        "bitter" to WordDefinition("Bitter", "/ˈbɪtə/", "adjective", "Having a sharp, pungent taste; feeling angry, hurt, or resentful.", "A bitter wind swept across the frozen moor.", listOf("sharp", "resentful", "harsh", "acrid")),
        "bleak" to WordDefinition("Bleak", "/bliːk/", "adjective", "Lacking vegetation and exposed to the elements; cold and miserable.", "The bleak landscape stretched endlessly toward the grey clouds.", listOf("barren", "grim", "desolate", "cheerless")),
        "blessing" to WordDefinition("Blessing", "/ˈblesɪŋ/", "noun", "God's favor and protection; a beneficial thing for which one is grateful.", "The sudden rainfall proved a blessing to the parched valley.", listOf("gift", "boon", "grace", "benefit")),
        "bold" to WordDefinition("Bold", "/bəʊld/", "adjective", "Showing an ability to take risks; confident and courageous.", "A bold plan formed in the young captain's mind.", listOf("brave", "daring", "fearless", "audacious")),
        "brave" to WordDefinition("Brave", "/breɪv/", "adjective", "Ready to face and endure danger or pain; showing courage.", "The brave traveler stepped into the shadowy passage.", listOf("courageous", "valiant", "heroic", "stouthearted")),
        "breeze" to WordDefinition("Breeze", "/briːz/", "noun", "A gentle, refreshing wind.", "A cool sea breeze fluttered the sails.", listOf("gust", "zephyr", "air", "draft")),
        "brilliant" to WordDefinition("Brilliant", "/ˈbrɪliənt/", "adjective", "Exceptionally clever or talented; very bright and radiant.", "Her brilliant deductions untangled the deepest mystery.", listOf("dazzling", "ingenious", "radiant", "sharp")),
        "burden" to WordDefinition("Burden", "/ˈbɜːdn/", "noun", "A load, especially a heavy one; something difficult to bear.", "He carried the heavy burden of leadership alone.", listOf("load", "weight", "responsibility", "trouble")),
        "calm" to WordDefinition("Calm", "/kɑːm/", "adjective / noun", "Not showing or feeling nervousness, anger, or other strong emotions; peace.", "Her voice remained calm despite the surrounding chaos.", listOf("serene", "tranquil", "peaceful", "placid")),
        "capture" to WordDefinition("Capture", "/ˈkæptʃə/", "verb", "To take into one's possession or control by force; to represent faithfully.", "The painting captures the fleeting golden hour of autumn.", listOf("seize", "record", "catch", "apprehend")),
        "celebrate" to WordDefinition("Celebrate", "/ˈselɪbreɪt/", "verb", "To acknowledge a significant event with a social gathering or enjoyable activity.", "The village gathered to celebrate the arrival of spring.", listOf("commemorate", "honor", "rejoice", "observe")),
        "ceremony" to WordDefinition("Ceremony", "/ˈserəməni/", "noun", "A formal religious or public occasion, typically one celebrating a particular event.", "The coronation ceremony drew onlookers from distant realms.", listOf("ritual", "rite", "service", "celebration")),
        "certain" to WordDefinition("Certain", "/ˈsɜːtn/", "adjective", "Known for sure; established beyond doubt.", "She was certain that morning would bring clarity.", listOf("sure", "confident", "positive", "definite")),
        "challenge" to WordDefinition("Challenge", "/ˈtʃælɪndʒ/", "noun / verb", "A call to take part in a contest or fight; a demanding task.", "Every chapter presented a fresh philosophical challenge.", listOf("test", "obstacle", "dare", "trial")),
        "chapter" to WordDefinition("Chapter", "/ˈtʃæptə/", "noun", "A main division of a book; a distinctive period in history or life.", "Closing the volume marked the end of an unforgettable chapter.", listOf("section", "part", "epoch", "division")),
        "character" to WordDefinition("Character", "/ˈkærəktə/", "noun", "The mental and moral qualities distinctive to an individual; a person in a novel.", "True character reveals itself in times of adversity.", listOf("personality", "nature", "figure", "integrity")),
        "charm" to WordDefinition("Charm", "/tʃɑːm/", "noun / verb", "The power or quality of delighting, attracting, or fascinating others.", "The quaint cottage held an unmistakable rustic charm.", listOf("allure", "appeal", "grace", "enchantment")),
        "cherish" to WordDefinition("Cherish", "/ˈtʃerɪʃ/", "verb", "To protect and care for someone lovingly; to hold dear in mind.", "They cherished every handwritten letter from home.", listOf("treasure", "prize", "hold dear", "value")),
        "clarity" to WordDefinition("Clarity", "/ˈklærəti/", "noun", "The quality of being clear, easily understood, or transparent.", "The explanation brought sudden clarity to the complex riddle.", listOf("lucidity", "clearness", "sharpness", "simplicity")),
        "clever" to WordDefinition("Clever", "/ˈklevə/", "adjective", "Quick to understand, learn, and devise or apply ideas.", "A clever device kept the lantern burning in the gale.", listOf("smart", "ingenious", "witty", "shrewd")),
        "comfort" to WordDefinition("Comfort", "/ˈkʌmfət/", "noun / verb", "A state of physical ease and freedom from pain or constraint; consolation.", "Her presence brought immense comfort to the grieving family.", listOf("solace", "consolation", "ease", "relief")),
        "compassion" to WordDefinition("Compassion", "/kəmˈpæʃn/", "noun", "Sympathetic pity and concern for the sufferings or misfortunes of others.", "He looked upon the travelers with genuine compassion.", listOf("empathy", "mercy", "kindness", "tenderness")),
        "conclude" to WordDefinition("Conclude", "/kənˈkluːd/", "verb", "To bring something to an end; to arrive at a judgment by reasoning.", "The story concludes with an unexpected twist.", listOf("finish", "deduce", "end", "resolve")),
        "confidence" to WordDefinition("Confidence", "/ˈkɒnfɪdəns/", "noun", "A feeling of self-assurance arising from one's appreciation of one's abilities.", "With growing confidence, she stepped forward to address the court.", listOf("assurance", "trust", "belief", "faith")),
        "conflict" to WordDefinition("Conflict", "/ˈkɒnflɪkt/", "noun", "A serious disagreement or argument; an active struggle between opposing forces.", "The central conflict of the drama lies between honor and ambition.", listOf("struggle", "dispute", "clash", "strife")),
        "conscience" to WordDefinition("Conscience", "/ˈkɒnʃəns/", "noun", "An inner feeling or voice viewed as acting as a guide to the rightness or wrongness.", "His troubled conscience gave him no rest that night.", listOf("morals", "principles", "integrity", "scruples")),
        "courage" to WordDefinition("Courage", "/ˈkʌrɪdʒ/", "noun", "The ability to do something that frightens one; bravery.", "It took immense courage to sail into the stormy strait.", listOf("bravery", "valor", "fearlessness", "grit")),
        "curious" to WordDefinition("Curious", "/ˈkjʊəriəs/", "adjective", "Eager to know or learn something; strange or unusual.", "The curious child examined the old clockwork with fascination.", listOf("inquisitive", "intrigued", "peculiar", "odd")),
        "darkness" to WordDefinition("Darkness", "/ˈdɑːknəs/", "noun", "The partial or total absence of light; gloom or obscurity.", "A single candle pushed back the encroaching darkness.", listOf("gloom", "shadow", "obscurity", "blackness")),
        "dawn" to WordDefinition("Dawn", "/dɔːn/", "noun / verb", "The first appearance of light in the sky before sunrise; a beginning.", "At the crack of dawn, the caravan set out.", listOf("daybreak", "sunrise", "beginning", "birth")),
        "dazzle" to WordDefinition("Dazzle", "/ˈdæzl/", "verb", "To blind someone temporarily with great brightness; to impress greatly.", "The golden halls dazzled the weary explorers.", listOf("blind", "astonish", "amaze", "mesmerize")),
        "deceive" to WordDefinition("Deceive", "/dɪˈsiːv/", "verb", "To cause someone to believe something that is not true, typically for personal gain.", "Appearances in the mirror world can easily deceive you.", listOf("mislead", "trick", "fool", "delude")),
        "decision" to WordDefinition("Decision", "/dɪˈsɪʒn/", "noun", "A conclusion or resolution reached after consideration.", "Making the right decision required weighing both past and future.", listOf("choice", "resolution", "judgment", "verdict")),
        "delicate" to WordDefinition("Delicate", "/ˈdelɪkət/", "adjective", "Very fine in texture or structure; easily broken or damaged.", "A delicate spiderweb hung between the rose branches.", listOf("fragile", "subtle", "exquisite", "tender")),
        "delight" to WordDefinition("Delight", "/dɪˈlaɪt/", "noun / verb", "Great pleasure or joy; to please greatly.", "A look of pure delight spread across the child's face.", listOf("joy", "pleasure", "gladness", "glee")),
        "desire" to WordDefinition("Desire", "/dɪˈzaɪə/", "noun / verb", "A strong feeling of wanting to have something or wishing for something to happen.", "His deepest desire was to return to his homeland.", listOf("wish", "craving", "longing", "yearning")),
        "destiny" to WordDefinition("Destiny", "/ˈdestɪni/", "noun", "The events that will necessarily happen to a particular person or thing in the future.", "She believed that their meeting was arranged by destiny.", listOf("fate", "fortune", "kismet", "providence")),
        "dignity" to WordDefinition("Dignity", "/ˈdɪɡnəti/", "noun", "The state or quality of being worthy of honor or respect.", "He bore his exile with quiet, unshakeable dignity.", listOf("nobility", "honor", "self-respect", "grace")),
        "dilemma" to WordDefinition("Dilemma", "/dɪˈlemə/", "noun", "A situation in which a difficult choice has to be made between alternatives.", "The hero faced a harrowing moral dilemma.", listOf("predicament", "quandary", "puzzle", "crisis")),
        "discover" to WordDefinition("Discover", "/dɪˈskʌvə/", "verb", "To find unexpectedly or during a search; to become aware of.", "In the attic, they discovered a chest of forgotten journals.", listOf("uncover", "find", "reveal", "locate")),
        "divine" to WordDefinition("Divine", "/dɪˈvaɪn/", "adjective", "Of, from, or like God or a god; excellent, delightful.", "A divine silence fell over the sacred sanctuary.", listOf("heavenly", "sacred", "celestial", "blessed")),
        "doubt" to WordDefinition("Doubt", "/daʊt/", "noun / verb", "A feeling of uncertainty or lack of conviction.", "A flicker of doubt crossed her mind, but she pressed on.", listOf("uncertainty", "hesitation", "suspicion", "disbelief")),
        "dramatic" to WordDefinition("Dramatic", "/drəˈmætɪk/", "adjective", "Relating to drama or the performance; sudden and striking.", "A dramatic change in the weather heralded the fleet's arrival.", listOf("striking", "theatrical", "vivid", "spectacular")),
        "dream" to WordDefinition("Dream", "/driːm/", "noun / verb", "A series of thoughts, images, and sensations occurring in a person's mind during sleep; an ideal.", "His dream was to establish a sanctuary for endangered books.", listOf("vision", "fantasy", "aspiration", "reverie")),
        "echo" to WordDefinition("Echo", "/ˈekəʊ/", "noun / verb", "A sound caused by the reflection of sound waves from a surface; a close parallel.", "The distant echo of church bells drifted over the lake.", listOf("resonance", "reverberation", "reflection", "repeat")),
        "eerie" to WordDefinition("Eerie", "/ˈɪəri/", "adjective", "Strange and frightening; suggestive of the supernatural.", "An eerie silence settled over the abandoned battlefield.", listOf("spooky", "uncanny", "creepy", "mysterious")),
        "elegant" to WordDefinition("Elegant", "/ˈelɪɡənt/", "adjective", "Pleasingly graceful and stylish in appearance or manner.", "Her prose possessed an elegant, understated rhythm.", listOf("graceful", "refined", "stylish", "poised")),
        "eloquent" to WordDefinition("Eloquent", "/ˈeləkwənt/", "adjective", "Fluent or persuasive in speaking or writing.", "An eloquent speech moved the entire council to action.", listOf("articulate", "expressive", "fluent", "persuasive")),
        "embrace" to WordDefinition("Embrace", "/ɪmˈbreɪs/", "verb", "To hold someone closely in one's arms; to accept or support willingly.", "They decided to embrace the uncertain journey ahead.", listOf("hug", "accept", "welcome", "clasp")),
        "emerge" to WordDefinition("Emerge", "/ɪˈmɜːdʒ/", "verb", "To move out of or away from something and become visible.", "The deer emerged cautiously from the dense thicket.", listOf("appear", "surface", "arise", "materialize")),
        "emotion" to WordDefinition("Emotion", "/ɪˈməʊʃn/", "noun", "A natural instinctive state of mind deriving from one's circumstances.", "A wave of sudden emotion overwhelmed the old traveler.", listOf("feeling", "sentiment", "passion", "reaction")),
        "endure" to WordDefinition("Endure", "/ɪnˈdjʊə/", "verb", "To suffer patiently; to remain in existence; to last.", "Great stories endure across centuries and cultures.", listOf("persevere", "survive", "withstand", "persist")),
        "enigma" to WordDefinition("Enigma", "/ɪˈnɪɡmə/", "noun", "A person or thing that is mysterious, puzzling, or difficult to understand.", "Her silent smile remained an enigma to all who met her.", listOf("mystery", "puzzle", "riddle", "conundrum")),
        "epiphany" to WordDefinition("Epiphany", "/ɪˈpɪfəni/", "noun", "A moment of sudden and profound revelation or insight.", "Sitting by the quiet stream, he experienced a life-changing epiphany.", listOf("revelation", "realization", "insight", "flash")),
        "escape" to WordDefinition("Escape", "/ɪˈskeɪp/", "verb / noun", "To break free from confinement or control; to avoid something.", "Reading allowed him to escape the dreary realities of daily work.", listOf("flee", "evade", "break out", "slip away")),
        "essence" to WordDefinition("Essence", "/ˈesns/", "noun", "The intrinsic nature or indispensable quality of something.", "Compassion is the very essence of true wisdom.", listOf("core", "heart", "substance", "spirit")),
        "eternal" to WordDefinition("Eternal", "/ɪˈtɜːnl/", "adjective", "Lasting or existing forever; without end or beginning.", "The stars shone with eternal, unhurried brilliance.", listOf("everlasting", "perpetual", "immortal", "timeless")),
        "ethereal" to WordDefinition("Ethereal", "/iˈθɪəriəl/", "adjective", "Extremely delicate and light in a way that seems not of this world.", "The mist gave the forest an ethereal, dreamlike glow.", listOf("delicate", "airy", "celestial", "heavenly")),
        "evidence" to WordDefinition("Evidence", "/ˈevɪdəns/", "noun", "The available body of facts or information indicating whether a belief is true.", "The traveler found unmistakable evidence of human habitation.", listOf("proof", "clues", "data", "confirmation")),
        "examine" to WordDefinition("Examine", "/ɪɡˈzæmɪn/", "verb", "To inspect in detail to determine the nature or condition.", "The scholar leaned in to examine the illuminated lettering.", listOf("inspect", "scrutinize", "investigate", "study")),
        "exquisite" to WordDefinition("Exquisite", "/ɪkˈskwɪzɪt/", "adjective", "Extremely beautiful and delicate; intense or acute.", "The jeweler produced an exquisite brooch set with emeralds.", listOf("magnificent", "superb", "delicate", "flawless")),
        "faint" to WordDefinition("Faint", "/feɪnt/", "adjective / verb", "Barely perceptible; weak and dizzy; to lose consciousness temporarily.", "A faint whisper of song drifted from the distant hall.", listOf("dim", "slight", "subtle", "indistinct")),
        "faith" to WordDefinition("Faith", "/feɪθ/", "noun", "Complete trust or confidence in someone or something; spiritual belief.", "Her unwavering faith sustained the expedition through the desert.", listOf("trust", "belief", "devotion", "conviction")),
        "famous" to WordDefinition("Famous", "/ˈfeɪməs/", "adjective", "Known about by many people; renowned.", "The city was famous for its ancient libraries and bridges.", listOf("renowned", "celebrated", "notable", "illustrious")),
        "fascinate" to WordDefinition("Fascinate", "/ˈfæsɪneɪt/", "verb", "To attract the strong attention and interest of someone.", "Old maps and astronomical charts never ceased to fascinate him.", listOf("captivate", "intrigue", "charm", "beguile")),
        "fate" to WordDefinition("Fate", "/feɪt/", "noun", "The development of events beyond a person's control, regarded as determined by a supernatural power.", "He refused to accept that defeat was his inescapable fate.", listOf("destiny", "fortune", "kismet", "doom")),
        "fear" to WordDefinition("Fear", "/fɪə/", "noun / verb", "An unpleasant emotion caused by the belief that someone is dangerous or likely to cause pain.", "Courage is not the absence of fear, but triumph over it.", listOf("dread", "terror", "apprehension", "anxiety")),
        "ferocious" to WordDefinition("Ferocious", "/fəˈrəʊʃəs/", "adjective", "Savagely fierce, cruel, or violent.", "A ferocious storm battered the coastline all through the night.", listOf("fierce", "savage", "wild", "intense")),
        "fierce" to WordDefinition("Fierce", "/fɪəs/", "adjective", "Having or displaying an intense or ferocious aggressiveness.", "A fierce pride burned in her eyes as she defended her home.", listOf("ferocious", "intense", "passionate", "furious")),
        "flame" to WordDefinition("Flame", "/fleɪm/", "noun", "A hot glowing body of ignited gas generated by something on fire; passion.", "A single flame danced upon the hearth, warming the chill room.", listOf("blaze", "fire", "spark", "ember")),
        "flourish" to WordDefinition("Flourish", "/ˈflʌrɪʃ/", "verb", "To grow or develop in a healthy or vigorous way; to thrive.", "Under careful tending, the ancient orchard began to flourish anew.", listOf("thrive", "prosper", "bloom", "succeed")),
        "forest" to WordDefinition("Forest", "/ˈfɒrɪst/", "noun", "A large area covered chiefly with trees and undergrowth.", "The deep forest concealed pathless hills and clear springs.", listOf("woods", "woodland", "jungle", "timberland")),
        "forgive" to WordDefinition("Forgive", "/fəˈɡɪv/", "verb", "To stop feeling angry or resentful towards someone for an offense.", "To forgive is often harder than to demand retribution.", listOf("pardon", "excuse", "absolve", "overlook")),
        "fragile" to WordDefinition("Fragile", "/ˈfrædʒaɪl/", "adjective", "Easily broken or damaged; delicate and vulnerable.", "The fragile parchment crumbled slightly at the edges.", listOf("delicate", "brittle", "frail", "vulnerable")),
        "freedom" to WordDefinition("Freedom", "/ˈfriːdəm/", "noun", "The power or right to act, speak, or think as one wants without hindrance.", "Freedom of thought was the guiding tenet of the academy.", listOf("liberty", "independence", "autonomy", "release")),
        "friend" to WordDefinition("Friend", "/frend/", "noun", "A person with whom one has a bond of mutual affection and trust.", "A faithful friend is a sturdy shelter in life's tempests.", listOf("companion", "ally", "comrade", "confidant")),
        "gaze" to WordDefinition("Gaze", "/ɡeɪz/", "verb / noun", "To look steadily and intently, especially in admiration, surprise, or thought.", "She stood on the balcony to gaze at the silver ocean.", listOf("stare", "look", "contemplate", "regard")),
        "generous" to WordDefinition("Generous", "/ˈdʒenərəs/", "adjective", "Showing a readiness to give more of something than is strictly necessary.", "The merchant proved generous to travelers passing through his town.", listOf("bountiful", "charitable", "benevolent", "unselfish")),
        "gentle" to WordDefinition("Gentle", "/ˈdʒentl/", "adjective", "Having or showing a mild, kind, or tender temperament or character.", "A gentle hand rested reassuringly on his shoulder.", listOf("mild", "tender", "kind", "soft")),
        "genuine" to WordDefinition("Genuine", "/ˈdʒenjuɪn/", "adjective", "Truly what something is said to be; authentic and sincere.", "Her words were marked by genuine warmth and honesty.", listOf("authentic", "sincere", "real", "true")),
        "glance" to WordDefinition("Glance", "/ɡlɑːns/", "verb / noun", "To take a brief or hurried look; a quick look.", "A furtive glance towards the doorway revealed the guard.", listOf("peek", "glimpse", "look", "scan")),
        "glory" to WordDefinition("Glory", "/ˈɡlɔːri/", "noun", "High renown or honor won by notable achievements; magnificent splendor.", "The setting sun covered the snowy mountain peak in golden glory.", listOf("splendor", "honor", "majesty", "renown")),
        "grace" to WordDefinition("Grace", "/ɡreɪs/", "noun", "Simple elegance or refinement of movement; courteous goodwill; divine assistance.", "She moved with effortless grace across the ballroom.", listOf("elegance", "poise", "kindness", "dignity")),
        "gratitude" to WordDefinition("Gratitude", "/ˈɡrætɪtjuːd/", "noun", "The quality of being thankful; readiness to show appreciation.", "He bowed his head in quiet gratitude for their aid.", listOf("thankfulness", "appreciation", "acknowledgment", "thanks")),
        "grief" to WordDefinition("Grief", "/ɡriːf/", "noun", "Deep sorrow, especially that caused by someone's death.", "Time softened the sharpest edges of her heavy grief.", listOf("sorrow", "mourning", "heartache", "sadness")),
        "harmony" to WordDefinition("Harmony", "/ˈhɑːməni/", "noun", "Agreement or concord; pleasing arrangement of parts.", "The villagers lived in peaceful harmony with the surrounding hills.", listOf("accord", "balance", "concord", "peace")),
        "haunt" to WordDefinition("Haunt", "/hɔːnt/", "verb", "To manifest itself regularly to someone; to linger persistingly in the memory.", "The mournful melody continued to haunt his quiet hours.", listOf("linger", "plague", "obsess", "trouble")),
        "hazard" to WordDefinition("Hazard", "/ˈhæzəd/", "noun", "A danger or risk; an obstacle.", "Fierce currents were the principal hazard of the northern strait.", listOf("peril", "risk", "danger", "jeopardy")),
        "heart" to WordDefinition("Heart", "/hɑːt/", "noun", "The organ pumping blood; the center of emotion, courage, or essence.", "Deep in her heart, she knew the journey would succeed.", listOf("core", "soul", "spirit", "center")),
        "heritage" to WordDefinition("Heritage", "/ˈherɪtɪdʒ/", "noun", "Property, traditions, or culture that may be inherited.", "Preserving their cultural heritage was their life's work.", listOf("legacy", "inheritance", "tradition", "birthright")),
        "hero" to WordDefinition("Hero", "/ˈhɪərəʊ/", "noun", "A person admired for courage, outstanding achievements, or noble qualities.", "In folklore, the unexpected hero often carries the humblest origins.", listOf("champion", "protagonist", "idol", "savior")),
        "hesitate" to WordDefinition("Hesitate", "/ˈhezɪteɪt/", "verb", "To pause before saying or doing something through uncertainty.", "He did not hesitate when duty demanded immediate action.", listOf("pause", "waver", "falter", "dither")),
        "hidden" to WordDefinition("Hidden", "/ˈhɪdn/", "adjective", "Kept out of sight; concealed or secret.", "A hidden staircase led down into the cool cellars.", listOf("concealed", "secret", "covert", "obscure")),
        "honor" to WordDefinition("Honor", "/ˈɒnə/", "noun / verb", "High respect; great esteem; adherence to what is morally right.", "To keep one's word is the very bedrock of honor.", listOf("integrity", "esteem", "reverence", "reputation")),
        "hope" to WordDefinition("Hope", "/həʊp/", "noun / verb", "A feeling of expectation and desire for a particular thing to happen.", "Hope flickered like a lantern in the gathering dark.", listOf("aspiration", "optimism", "faith", "expectation")),
        "horizon" to WordDefinition("Horizon", "/həˈraɪzn/", "noun", "The line at which the earth's surface and the sky appear to meet; limit of experience.", "A cluster of sails appeared on the sunlit horizon.", listOf("skyline", "boundary", "vista", "reach")),
        "humble" to WordDefinition("Humble", "/ˈhʌmbl/", "adjective", "Having or showing a modest or low estimate of one's importance.", "Despite his great fame, the master remained quiet and humble.", listOf("modest", "unassuming", "meek", "respectful")),
        "illumine" to WordDefinition("Illumine", "/ɪˈljuːmɪn/", "verb", "To light up; to brighten with intellectual or spiritual light.", "Wise words illumine the shadowed corners of human nature.", listOf("light", "clarify", "illuminate", "brighten")),
        "illuminate" to WordDefinition("Illuminate", "/ɪˈluːmɪneɪt/", "verb", "To light up and make visible; to clarify or explain.", "Lanterns illuminated the garden path with a warm, welcoming light.", listOf("brighten", "clarify", "enlighten", "elucidate")),
        "illusion" to WordDefinition("Illusion", "/ɪˈluːʒn/", "noun", "A deceptive appearance or impression; a false idea or belief.", "The distant oasis was merely a heat illusion on the sands.", listOf("mirage", "delusion", "fantasy", "fallacy")),
        "imagine" to WordDefinition("Imagine", "/ɪˈmædʒɪn/", "verb", "To form a mental image or concept of; to suppose or believe.", "It was easy to imagine ancient ships gliding across the fjord.", listOf("envision", "picture", "conceive", "fathom")),
        "immense" to WordDefinition("Immense", "/ɪˈmens/", "adjective", "Extremely large or great, especially in scale or degree.", "The immense cavern echoed with the sound of dripping water.", listOf("vast", "enormous", "colossal", "huge")),
        "immortal" to WordDefinition("Immortal", "/ɪˈmɔːtl/", "adjective", "Living forever; never dying or decaying; enduring.", "The immortal verses of Homer have crossed three millennia.", listOf("undying", "eternal", "perpetual", "enduring")),
        "innocence" to WordDefinition("Innocence", "/ˈɪnəsns/", "noun", "The state, quality, or fact of being innocent of a crime or offense.", "The quiet innocence of youth radiated from his eyes.", listOf("purity", "guilelessness", "naivety", "integrity")),
        "insight" to WordDefinition("Insight", "/ˈɪnsaɪt/", "noun", "The capacity to gain an accurate and deep understanding of someone or something.", "The philosopher shared profound insight into the human condition.", listOf("discernment", "perception", "wisdom", "understanding")),
        "inspire" to WordDefinition("Inspire", "/ɪnˈspaɪə/", "verb", "To fill someone with the urge or ability to do or feel something, especially creative.", "The mountain sunrise inspired an entire chapter of verses.", listOf("motivate", "encourage", "kindle", "stimulate")),
        "integrity" to WordDefinition("Integrity", "/ɪnˈteɡrəti/", "noun", "The quality of being honest and having strong moral principles.", "Her integrity won the trust of even her bitterest opponents.", listOf("honesty", "probity", "honor", "rectitude")),
        "intense" to WordDefinition("Intense", "/ɪnˈtens/", "adjective", "Of extreme force, degree, or strength; possessing strong feeling.", "The intense cold turned the travelers' breath to frost.", listOf("acute", "fierce", "profound", "powerful")),
        "journey" to WordDefinition("Journey", "/ˈdʒɜːni/", "noun / verb", "An act of traveling from one place to another; personal change.", "Their long journey across the wilderness tested both heart and resolve.", listOf("voyage", "expedition", "pilgrimage", "passage")),
        "justice" to WordDefinition("Justice", "/ˈdʒʌstɪs/", "noun", "Just behavior or treatment; the quality of being fair and reasonable.", "The ruler was celebrated far and wide for his unwavering justice.", listOf("fairness", "equity", "right", "honor")),
        "legend" to WordDefinition("Legend", "/ˈledʒənd/", "noun", "A traditional story sometimes regarded as historical but unauthenticated.", "Old sailors whisper legends of islands that drift like clouds.", listOf("myth", "folklore", "tale", "fable")),
        "liberty" to WordDefinition("Liberty", "/ˈlɪbəti/", "noun", "The state of being free within society from oppressive restrictions.", "The monument stood as an enduring symbol of human liberty.", listOf("freedom", "independence", "emancipation", "autonomy")),
        "literature" to WordDefinition("Literature", "/ˈlɪtrətʃə/", "noun", "Written works, especially those considered of superior or lasting artistic merit.", "Reading great literature broadens the boundaries of the soul.", listOf("writings", "letters", "books", "prose")),
        "luminous" to WordDefinition("Luminous", "/ˈluːmɪnəs/", "adjective", "Emitting or reflecting steady, glowing light; inspiring or enlightened.", "The moon cast a luminous glow over the calm water.", listOf("radiant", "glowing", "bright", "shining")),
        "magic" to WordDefinition("Magic", "/ˈmædʒɪk/", "noun / adjective", "The power of apparently influencing events by mysterious or supernatural forces.", "There was true magic in the warmth of the roaring hearth.", listOf("sorcery", "enchantment", "wonder", "charm")),
        "magnificent" to WordDefinition("Magnificent", "/mæɡˈnɪfɪsnt/", "adjective", "Impressively beautiful, elaborate, or striking; noble.", "The travelers gazed upon the magnificent peaks rising into the dawn.", listOf("splendid", "grand", "majestic", "glorious")),
        "majesty" to WordDefinition("Majesty", "/ˈmædʒəsti/", "noun", "Impressive stateliness, dignity, or beauty.", "The great redwood forest possessed an awe-inspiring majesty.", listOf("grandeur", "dignity", "nobility", "glory")),
        "melancholy" to WordDefinition("Melancholy", "/ˈmelənkəli/", "noun / adjective", "A feeling of pensive sadness, typically with no obvious cause.", "A soft melancholy drifted through the melody of the cello.", listOf("sorrow", "pensiveness", "gloom", "wistfulness")),
        "memory" to WordDefinition("Memory", "/ˈmeməri/", "noun", "The faculty by which the mind stores and remembers information; recollection.", "A tender memory of childhood warmed his lonely night.", listOf("recollection", "reminiscence", "remembrance", "mind")),
        "mercy" to WordDefinition("Mercy", "/ˈmɜːsi/", "noun", "Compassion or forgiveness shown towards someone whom it is within one's power to punish.", "The victorious commander showed unexpected mercy to the captives.", listOf("clemency", "compassion", "forgiveness", "grace")),
        "miracle" to WordDefinition("Miracle", "/ˈmɪrəkl/", "noun", "A remarkable event or development that brings very welcome consequences.", "Finding their way out of the blizzard was nothing short of a miracle.", listOf("wonder", "marvel", "phenomenon", "boon")),
        "mystery" to WordDefinition("Mystery", "/ˈmɪstəri/", "noun", "Something that is difficult or impossible to understand or explain.", "The origin of the ancient map remained an unsolved mystery.", listOf("enigma", "puzzle", "riddle", "secret")),
        "myth" to WordDefinition("Myth", "/mɪθ/", "noun", "A traditional story concerning early history or explaining a natural phenomenon.", "Ancient myths preserved the wisdom of generations.", listOf("legend", "fable", "folklore", "tale")),
        "nature" to WordDefinition("Nature", "/ˈneɪtʃə/", "noun", "The phenomena of the physical world collectively; essential qualities.", "In the quiet woods, she felt at one with nature.", listOf("creation", "earth", "character", "essence")),
        "noble" to WordDefinition("Noble", "/ˈnəʊbl/", "adjective", "Belonging to a hereditary class with high social status; having fine personal qualities.", "He made a noble sacrifice for the sake of his companions.", listOf("magnanimous", "honorable", "distinguished", "worthy")),
        "nostalgia" to WordDefinition("Nostalgia", "/nɒˈstældʒə/", "noun", "A sentimental longing or wistful affection for the past.", "Listening to the old lullaby stirred a sweet nostalgia.", listOf("reminiscence", "wistfulness", "longing", "yearning")),
        "novel" to WordDefinition("Novel", "/ˈnɒvl/", "noun / adjective", "A fictitious prose narrative of book length; new and original.", "She spent the winter reading a classic Russian novel.", listOf("book", "tale", "story", "original")),
        "observe" to WordDefinition("Observe", "/əbˈzɜːv/", "verb", "To notice or perceive something and register it as significant.", "The naturalist paused to observe the rare songbird's flight.", listOf("notice", "watch", "witness", "examine")),
        "oracle" to WordDefinition("Oracle", "/ˈɒrəkl/", "noun", "A priest or priestess acting as a medium through whom advice or prophecy was sought.", "Kings traveled across kingdoms to consult the mountain oracle.", listOf("prophet", "seer", "sage", "diviner")),
        "paradise" to WordDefinition("Paradise", "/ˈpærədaɪs/", "noun", "An ideal or idyllic place or state; heaven.", "The secluded valley seemed like an untouched paradise on earth.", listOf("eden", "heaven", "utopia", "elysium")),
        "paradox" to WordDefinition("Paradox", "/ˈpærədɒks/", "noun", "A seemingly absurd or self-contradictory statement that may prove true.", "The ancient paradox taught that to gain one's life, one must surrender it.", listOf("enigma", "contradiction", "puzzle", "riddle")),
        "passion" to WordDefinition("Passion", "/ˈpæʃn/", "noun", "Strong and barely controllable emotion; an intense enthusiasm.", "Her passion for astronomy carried her through long nights of study.", listOf("fervor", "zeal", "ardor", "enthusiasm")),
        "patience" to WordDefinition("Patience", "/ˈpeɪʃns/", "noun", "The capacity to accept or tolerate delay, trouble, or suffering without getting angry.", "Patience and perseverance conquer all formidable obstacles.", listOf("endurance", "forbearance", "fortitude", "calm")),
        "peace" to WordDefinition("Peace", "/piːs/", "noun", "Freedom from disturbance; tranquility; state of mutual concord.", "A serene peace descended upon the sleepy village.", listOf("tranquility", "serenity", "calm", "accord")),
        "perseverance" to WordDefinition("Perseverance", "/ˌpɜːsɪˈvɪərəns/", "noun", "Persistence in doing something despite difficulty or delay in achieving success.", "Through patient perseverance, the scholar deciphered the tablet.", listOf("persistence", "tenacity", "dedication", "grit")),
        "ponder" to WordDefinition("Ponder", "/ˈpɒndə/", "verb", "To think about something carefully, especially before making a decision.", "He walked along the shore to ponder the merchant's proposal.", listOf("contemplate", "meditate", "reflect", "consider")),
        "profound" to WordDefinition("Profound", "/prəˈfaʊnd/", "adjective", "Very great or intense; having or showing great insight.", "The poem expressed a profound sense of wonder toward the universe.", listOf("deep", "insightful", "intense", "far-reaching")),
        "quest" to WordDefinition("Quest", "/kwest/", "noun", "A long or arduous search for something.", "The knight set out upon an epic quest across the sea.", listOf("search", "expedition", "journey", "pursuit")),
        "radiant" to WordDefinition("Radiant", "/ˈreɪdiənt/", "adjective", "Sending out light; shining or glowing brightly; beaming with joy.", "A radiant smile illuminated her countenance.", listOf("glowing", "shining", "beaming", "dazzling")),
        "refuge" to WordDefinition("Refuge", "/ˈrefjuːdʒ/", "noun", "A condition of being safe or sheltered from pursuit, danger, or trouble.", "The mountain cabin offered a welcome refuge from the blizzard.", listOf("shelter", "sanctuary", "haven", "retreat")),
        "resilience" to WordDefinition("Resilience", "/rɪˈzɪliəns/", "noun", "The capacity to withstand or recover quickly from difficulties; toughness.", "The community displayed remarkable courage and resilience through hard times.", listOf("toughness", "endurance", "strength", "perseverance")),
        "sacred" to WordDefinition("Sacred", "/ˈseɪkrɪd/", "adjective", "Connected with God or dedicated to a religious purpose; holy.", "The sacred grove was honored by all neighboring towns.", listOf("holy", "hallowed", "blessed", "divine")),
        "sanctuary" to WordDefinition("Sanctuary", "/ˈsæŋktʃuəri/", "noun", "A place of safety or refuge; a holy place.", "The ancient library served as a quiet sanctuary for thinkers.", listOf("refuge", "haven", "retreat", "shelter")),
        "serendipity" to WordDefinition("Serendipity", "/ˌserənˈdɪpəti/", "noun", "The occurrence of events by chance in a happy or beneficial way.", "Finding that rare first edition in a market stall was pure serendipity.", listOf("happy accident", "fluke", "good fortune", "blessing")),
        "serene" to WordDefinition("Serene", "/səˈriːn/", "adjective", "Calm, peaceful, and untroubled; tranquil.", "The serene waters reflected the snow-capped mountain peaks.", listOf("peaceful", "tranquil", "placid", "calm")),
        "shadow" to WordDefinition("Shadow", "/ˈʃædəʊ/", "noun / verb", "A dark area produced by a body blocking light; a trace or hint.", "Long shadows stretched across the quiet meadow at dusk.", listOf("shade", "silhouette", "darkness", "trace")),
        "silence" to WordDefinition("Silence", "/ˈsaɪləns/", "noun", "Complete absence of sound; a period without speech.", "A respectful silence enveloped the ancient chapel.", listOf("stillness", "quiet", "hush", "peace")),
        "solitude" to WordDefinition("Solitude", "/ˈsɒlɪtjuːd/", "noun", "The state of being alone, especially when pleasant or contemplative.", "She found creative inspiration in the quiet solitude of her studio.", listOf("seclusion", "privacy", "peace", "aloneness")),
        "sorrow" to WordDefinition("Sorrow", "/ˈsɒrəʊ/", "noun", "A feeling of deep distress caused by loss, disappointment, or grief.", "Music was the only remedy that eased his quiet sorrow.", listOf("grief", "sadness", "mourning", "woe")),
        "splendid" to WordDefinition("Splendid", "/ˈsplendɪd/", "adjective", "Magnificent; very impressive; excellent.", "A splendid array of lanterns decorated the riverbank.", listOf("magnificent", "grand", "glorious", "superb")),
        "subtle" to WordDefinition("Subtle", "/ˈsʌtl/", "adjective", "So delicate or precise as to be difficult to analyze or describe; understated.", "There was a subtle irony hidden beneath his polite words.", listOf("delicate", "understated", "fine", "elusive")),
        "tranquil" to WordDefinition("Tranquil", "/ˈtræŋkwɪl/", "adjective", "Free from disturbance; calm, serene, and peaceful.", "The tranquil morning air carried the scent of pine needles.", listOf("peaceful", "serene", "calm", "placid")),
        "twilight" to WordDefinition("Twilight", "/ˈtwaɪlaɪt/", "noun", "The soft glowing light from the sky when the sun is below the horizon.", "They strolled through the village during the gentle summer twilight.", listOf("dusk", "gloaming", "sunset", "evening")),
        "understand" to WordDefinition("Understand", "/ˌʌndəˈstænd/", "verb", "To perceive the intended meaning of words, a language, or a person.", "To understand another is the first step toward true fellowship.", listOf("comprehend", "grasp", "fathom", "perceive")),
        "valiant" to WordDefinition("Valiant", "/ˈvæliənt/", "adjective", "Possessing or showing courage or determination.", "The valiant guard held the gate until the villagers were safe.", listOf("brave", "courageous", "heroic", "fearless")),
        "vanish" to WordDefinition("Vanish", "/ˈvænɪʃ/", "verb", "To disappear suddenly and completely.", "The ghost ship vanished into the dense white sea fog.", listOf("disappear", "evaporate", "dissolve", "fade")),
        "venture" to WordDefinition("Venture", "/ˈventʃə/", "noun / verb", "A risky or daring journey or undertaking; to dare to do something.", "They ventured deeper into the uncharted mountain pass.", listOf("dare", "undertaking", "adventure", "enterprise")),
        "victory" to WordDefinition("Victory", "/ˈvɪktəri/", "noun", "An act of defeating an enemy or opponent in a battle or competition.", "The victory was celebrated with songs that echoed through the valley.", listOf("triumph", "conquest", "success", "win")),
        "virtue" to WordDefinition("Virtue", "/ˈvɜːtʃuː/", "noun", "Behavior showing high moral standards; a good or useful quality.", "Patience is a virtue revered in classical philosophy.", listOf("goodness", "morality", "integrity", "rectitude")),
        "wander" to WordDefinition("Wander", "/ˈwɒndə/", "verb", "To walk or move in a leisurely, casual, or aimless way.", "They wandered through the winding cobblestone alleys of the old city.", listOf("roam", "meander", "stroll", "drift")),
        "whisper" to WordDefinition("Whisper", "/ˈwɪspə/", "verb / noun", "To speak very softly using one's breath without vocal cord vibration.", "The breeze whispered ancient secrets among the tall trees.", listOf("murmur", "mutter", "rustle", "breathe")),
        "wilderness" to WordDefinition("Wilderness", "/ˈwɪldənəs/", "noun", "An uncultivated, uninhabited, and hospitable region.", "Only seasoned trackers dared cross the northern wilderness.", listOf("wilds", "wasteland", "forest", "backcountry")),
        "wisdom" to WordDefinition("Wisdom", "/ˈwɪzdəm/", "noun", "The quality of having experience, knowledge, and good judgment.", "His quiet words carried the wisdom of many generations.", listOf("insight", "discernment", "prudence", "sagacity")),
        "wonder" to WordDefinition("Wonder", "/ˈwʌndə/", "noun / verb", "A feeling of amazement and admiration, caused by something beautiful or unexpected.", "The starry night sky filled the travelers with childlike wonder.", listOf("awe", "amazement", "curiosity", "marvel")),
        "yearn" to WordDefinition("Yearn", "/jɜːn/", "verb", "To have an intense feeling of longing for something, typically lost or distant.", "In the crowded city, she yearned for the quiet green hills of home.", listOf("long", "crave", "pine", "desire")),
        "zeal" to WordDefinition("Zeal", "/ziːl/", "noun", "Great energy or enthusiasm in pursuit of a cause or an objective.", "He tackled the translation of the ancient codex with unflagging zeal.", listOf("passion", "enthusiasm", "fervor", "ardor"))
    )

    fun cleanWord(raw: String): String {
        return raw.trim()
            .replace(Regex("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$"), "")
            .lowercase()
    }

    fun lookup(rawWord: String): WordDefinition {
        val cleaned = cleanWord(rawWord)
        if (cleaned.isBlank()) {
            return WordDefinition(
                word = rawWord,
                phonetic = "/.../",
                partOfSpeech = "term",
                definition = "Select or long-press any word in the text to see its meaning.",
                example = ""
            )
        }

        // 1. Direct match in dictionary
        localDictionary[cleaned]?.let { return it }

        // 2. Common grammatical suffix stripping / lemma check (plurals, tenses, adverbs)
        val candidateRoots = listOf(
            cleaned.removeSuffix("s"),
            cleaned.removeSuffix("es"),
            cleaned.removeSuffix("ed"),
            cleaned.removeSuffix("ing"),
            cleaned.removeSuffix("d"),
            cleaned.removeSuffix("ly"),
            cleaned.removeSuffix("ness"),
            cleaned.removeSuffix("ment"),
            cleaned.removeSuffix("ful"),
            cleaned.removeSuffix("less"),
            cleaned.removeSuffix("able"),
            cleaned.removeSuffix("ible"),
            cleaned.removeSuffix("est"),
            cleaned.removeSuffix("er")
        )
        for (cand in candidateRoots) {
            if (cand.length >= 3) {
                localDictionary[cand]?.let {
                    val capitalized = rawWord.trim().replaceFirstChar { c -> c.uppercase() }
                    return it.copy(word = capitalized)
                }
            }
        }

        // 3. Intelligent linguistic breakdown for terms not explicitly in basic table
        val capitalized = cleaned.replaceFirstChar { it.uppercase() }
        val inferredPos = when {
            cleaned.endsWith("ly") -> "adverb"
            cleaned.endsWith("ing") || cleaned.endsWith("ed") -> "verb / participle"
            cleaned.endsWith("ous") || cleaned.endsWith("ful") || cleaned.endsWith("ive") || cleaned.endsWith("able") || cleaned.endsWith("al") -> "adjective"
            cleaned.endsWith("tion") || cleaned.endsWith("ness") || cleaned.endsWith("ment") || cleaned.endsWith("ity") || cleaned.endsWith("ism") -> "noun"
            else -> "noun / verb"
        }

        return WordDefinition(
            word = capitalized,
            phonetic = "/${cleaned}/",
            partOfSpeech = inferredPos,
            definition = "Literary word appearing in this passage. Represents a key thematic element or descriptor in the narrative.",
            example = "Occurs in context: \"... $rawWord ...\"",
            synonyms = listOf("context", "reading", "expression")
        )
    }
}
