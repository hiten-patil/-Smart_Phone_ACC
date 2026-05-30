// Phone Comparison JavaScript

let phone1Data = null;
let phone2Data = null;

// Load brands on page load
document.addEventListener('DOMContentLoaded', function() {
    loadBrands();
    setupEventListeners();
});

// Setup event listeners
function setupEventListeners() {
    // Brand 1 change
    document.getElementById('brand1').addEventListener('change', function() {
        const brand = this.value;
        if (brand) {
            loadModels(brand, 1);
        } else {
            document.getElementById('model1').innerHTML = '<option value="">-- Choose Model --</option>';
            document.getElementById('model1').disabled = true;
            hidePhoneDetails(1);
        }
    });

    // Brand 2 change
    document.getElementById('brand2').addEventListener('change', function() {
        const brand = this.value;
        if (brand) {
            loadModels(brand, 2);
        } else {
            document.getElementById('model2').innerHTML = '<option value="">-- Choose Model --</option>';
            document.getElementById('model2').disabled = true;
            hidePhoneDetails(2);
        }
    });

    // Model 1 change
    document.getElementById('model1').addEventListener('change', function() {
        const phoneId = this.value;
        if (phoneId) {
            loadPhoneDetails(phoneId, 1);
        } else {
            hidePhoneDetails(1);
        }
    });

    // Model 2 change
    document.getElementById('model2').addEventListener('change', function() {
        const phoneId = this.value;
        if (phoneId) {
            loadPhoneDetails(phoneId, 2);
        } else {
            hidePhoneDetails(2);
        }
    });
}

// Load all brands
async function loadBrands() {
    try {
        const response = await fetch('/api/brands');
        const brands = await response.json();
        
        const brand1Select = document.getElementById('brand1');
        const brand2Select = document.getElementById('brand2');
        
        // Clear existing options except first
        brand1Select.innerHTML = '<option value="">-- Choose Brand --</option>';
        brand2Select.innerHTML = '<option value="">-- Choose Brand --</option>';
        
        // Add brand options
        brands.forEach(brand => {
            const option1 = document.createElement('option');
            option1.value = brand;
            option1.textContent = brand;
            brand1Select.appendChild(option1);
            
            const option2 = document.createElement('option');
            option2.value = brand;
            option2.textContent = brand;
            brand2Select.appendChild(option2);
        });
    } catch (error) {
        console.error('Error loading brands:', error);
        showError('Failed to load brands. Please refresh the page.');
    }
}

// Load models for selected brand
async function loadModels(brand, phoneNumber) {
    try {
        const response = await fetch(`/api/phones?brand=${encodeURIComponent(brand)}`);
        const phones = await response.json();
        
        const modelSelect = document.getElementById(`model${phoneNumber}`);
        modelSelect.innerHTML = '<option value="">-- Choose Model --</option>';
        
        phones.forEach(phone => {
            const option = document.createElement('option');
            option.value = phone.id;
            option.textContent = phone.model;
            modelSelect.appendChild(option);
        });
        
        modelSelect.disabled = false;
    } catch (error) {
        console.error('Error loading models:', error);
        showError('Failed to load models. Please try again.');
    }
}

// Load phone details
async function loadPhoneDetails(phoneId, phoneNumber) {
    try {
        console.log(`Loading phone details for ID: ${phoneId}, Phone Number: ${phoneNumber}`);
        const response = await fetch(`/api/phone/${phoneId}`);
        
        console.log('Response status:', response.status);
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const phone = await response.json();
        console.log('Phone data received:', phone);
        
        if (phoneNumber === 1) {
            phone1Data = phone;
        } else {
            phone2Data = phone;
        }
        
        displayPhoneDetails(phone, phoneNumber);
    } catch (error) {
        console.error('Error loading phone details:', error);
        showError(`Failed to load phone details: ${error.message}`);
    }
}

// Display phone details
function displayPhoneDetails(phone, phoneNumber) {
    const prefix = `phone${phoneNumber}`;
    
    // Show details section
    document.getElementById(`${prefix}-details`).style.display = 'block';
    
    // Set phone name and price
    document.getElementById(`${prefix}-name`).textContent = `${phone.brand} ${phone.model}`;
    document.getElementById(`${prefix}-price`).textContent = phone.priceCAD ? `$${phone.priceCAD} CAD` : 'Price not available';
    
    // Set specs
    document.getElementById(`${prefix}-processor`).textContent = phone.processor || 'N/A';
    document.getElementById(`${prefix}-ram`).textContent = phone.ram || 'N/A';
    document.getElementById(`${prefix}-storage`).textContent = phone.storage || 'N/A';
    document.getElementById(`${prefix}-display`).textContent = phone.displaySize || 'N/A';
    document.getElementById(`${prefix}-refresh`).textContent = phone.refreshRate || 'N/A';
    document.getElementById(`${prefix}-camera`).textContent = phone.cameraFeatures || 'N/A';
    document.getElementById(`${prefix}-battery`).textContent = phone.batteryCapacity || 'N/A';
    document.getElementById(`${prefix}-os`).textContent = phone.os || 'N/A';
    document.getElementById(`${prefix}-connectivity`).textContent = phone.connectivity || 'N/A';
    document.getElementById(`${prefix}-features`).textContent = phone.specialFeatures || 'N/A';
    
    // Set image (hide if no image) - only if image element exists
    const imgElement = document.getElementById(`${prefix}-image`);
    if (imgElement) {
        if (phone.imageUrl && phone.imageUrl.trim() !== '') {
            imgElement.src = phone.imageUrl;
            imgElement.style.display = 'block';
        } else {
            imgElement.style.display = 'none';
        }
    }
    
    // Highlight differences if both phones are loaded
    if (phone1Data && phone2Data) {
        highlightDifferences();
    }
}

// Hide phone details
function hidePhoneDetails(phoneNumber) {
    document.getElementById(`phone${phoneNumber}-details`).style.display = 'none';
    
    if (phoneNumber === 1) {
        phone1Data = null;
    } else {
        phone2Data = null;
    }
}

// Highlight differences between phones
function highlightDifferences() {
    // Highlighting disabled - no background colors shown
    return;
}

// Show error message
function showError(message) {
    // You can implement a toast or alert here
    console.error(message);
    alert(message);
}
